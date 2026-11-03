package dev.jdtech.jellyfin.work

import android.app.DownloadManager
import android.content.Context
import android.os.StatFs
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidMovie
import dev.jdtech.jellyfin.models.toFindroidUserDataDto
import dev.jdtech.jellyfin.repository.JellyfinRepositoryImpl
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.utils.DownloaderImpl
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

/**
 * Keeps the next unwatched episodes of every show in "Next up" and a few movies downloaded, and
 * removes downloaded items once they are watched, on this device or any other.
 */
@HiltWorker
class SmartDownloadsWorker
@AssistedInject
constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
    private val jellyfinApi: JellyfinApi,
    private val repository: JellyfinRepositoryImpl,
    private val workManager: WorkManager,
) : CoroutineWorker(appContext, params) {

    // Built here rather than injected so downloads always go through the online repository,
    // even when the app itself is in offline mode.
    private val downloader: Downloader by lazy {
        DownloaderImpl(appContext, database, repository, appPreferences, workManager)
    }

    // The downloader only checks one file at a time, but a run queues several of them.
    private var queuedBytes = 0L

    override suspend fun doWork(): Result {
        if (!appPreferences.getValue(appPreferences.smartDownloads)) return Result.success()
        val serverId =
            appPreferences.getValue(appPreferences.currentServer) ?: return Result.success()
        val userId = jellyfinApi.userId ?: return Result.success()

        return withContext(Dispatchers.IO) {
            runMutex.withLock {
                try {
                    val downloadedItems = getDownloadedItems(serverId, userId)
                    // Progress made offline must reach the server before we decide what is watched.
                    pushPendingUserData(database, jellyfinApi, userId, downloadedItems)
                    removeFailedDownloads(downloadedItems)
                    deleteWatchedItems(userId, downloadedItems)
                    downloadUpcomingEpisodes(userId)
                    updateMovies(serverId, userId)
                    Result.success()
                } catch (e: Exception) {
                    Timber.e(e)
                    Result.retry()
                }
            }
        }
    }

    private suspend fun getDownloadedItems(serverId: String, userId: UUID): List<FindroidItem> =
        database.getEpisodesByServerId(serverId).map { it.toFindroidEpisode(database, userId) } +
            database.getMoviesByServerId(serverId).map { it.toFindroidMovie(database, userId) }

    /** Lets the next run retry downloads that failed, for example when storage ran out. */
    private suspend fun removeFailedDownloads(items: List<FindroidItem>) {
        for (item in items) {
            val localSource = item.localSource() ?: continue
            // A finished download is renamed, only unfinished ones keep the .download suffix.
            if (!localSource.path.endsWith(".download")) continue

            val (status, _) = downloader.getProgress(localSource.downloadId)
            if (status == DownloadManager.STATUS_FAILED) {
                downloader.deleteItem(item, localSource)
            }
        }
    }

    private suspend fun deleteWatchedItems(userId: UUID, items: List<FindroidItem>) {
        for (item in items) {
            val localSource = item.localSource() ?: continue
            // The item may have been removed from the server, keep the download in that case.
            val remoteItem =
                try {
                    repository.getItem(item.id)
                } catch (_: Exception) {
                    null
                } ?: continue

            when {
                remoteItem.played -> downloader.deleteItem(item, localSource)
                database.getUserDataToBeSynced(userId, item.id) == null ->
                    database.insertUserData(remoteItem.toFindroidUserDataDto(userId))
            }
        }
    }

    private suspend fun downloadUpcomingEpisodes(userId: UUID) {
        val episodeCount = appPreferences.getValue(appPreferences.smartDownloadsEpisodeCount)
        if (episodeCount <= 0) return

        val nextUpEpisodes =
            jellyfinApi.showApi
                .getNextUp(userId, limit = NEXT_UP_LIMIT, enableResumable = true)
                .content
                .items

        for (nextUp in nextUpEpisodes) {
            val seriesId = nextUp.seriesId ?: continue
            // Without a season, the server returns episodes across seasons, starting at next up.
            val upcomingEpisodes =
                jellyfinApi.showApi
                    .getEpisodes(
                        seriesId,
                        userId,
                        startItemId = nextUp.id,
                        limit = episodeCount + SKIPPED_EPISODES_MARGIN,
                    )
                    .content
                    .items
                    .filter {
                        it.userData?.played != true && it.locationType != LocationType.VIRTUAL
                    }
                    .take(episodeCount)

            for (upcoming in upcomingEpisodes) {
                if (database.getSources(upcoming.id).isNotEmpty()) continue

                when (download(repository.getEpisode(upcoming.id))) {
                    DownloadResult.QUEUED,
                    DownloadResult.UNAVAILABLE -> Unit
                    DownloadResult.NO_ROOM,
                    DownloadResult.FAILED -> return
                }
            }
        }
    }

    /**
     * Keeps the wanted movies downloaded. Movies downloaded by hand are left alone until watched,
     * so only the ones this worker downloaded are tracked and replaced.
     */
    private suspend fun updateMovies(serverId: String, userId: UUID) {
        val movieCount = appPreferences.getValue(appPreferences.smartDownloadsMovieCount)
        val wantedIds = if (movieCount > 0) getWantedMovieIds(userId, movieCount) else emptyList()
        val smartIds =
            appPreferences.getValue(appPreferences.smartDownloadedMovieIds).toMutableSet()

        for (movie in database.getMoviesByServerId(serverId)) {
            if (movie.id.toString() !in smartIds || movie.id in wantedIds) continue
            val findroidMovie = movie.toFindroidMovie(database, userId)
            findroidMovie.localSource()?.let { downloader.deleteItem(findroidMovie, it) }
        }
        smartIds.retainAll(database.getMoviesByServerId(serverId).map { it.id.toString() }.toSet())

        for (movieId in wantedIds) {
            if (database.getSources(movieId).isNotEmpty()) continue

            when (download(repository.getMovie(movieId))) {
                DownloadResult.QUEUED -> smartIds += movieId.toString()
                DownloadResult.UNAVAILABLE,
                DownloadResult.NO_ROOM -> Unit
                DownloadResult.FAILED -> break
            }
        }
        appPreferences.setValue(appPreferences.smartDownloadedMovieIds, smartIds)
    }

    /** Movies in progress first, then unwatched favorites, then the latest unwatched additions. */
    private suspend fun getWantedMovieIds(userId: UUID, movieCount: Int): List<UUID> {
        val inProgress =
            jellyfinApi.libraryApi
                .getResumeItems(
                    userId,
                    limit = movieCount,
                    includeItemTypes = listOf(BaseItemKind.MOVIE),
                )
                .content
                .items
        val favorites = getUnplayedMovies(userId, movieCount, listOf(ItemFilter.IS_FAVORITE))
        val latest = getUnplayedMovies(userId, movieCount, emptyList())

        return (inProgress + favorites + latest).map { it.id }.distinct().take(movieCount)
    }

    private suspend fun getUnplayedMovies(
        userId: UUID,
        limit: Int,
        filters: List<ItemFilter>,
    ): List<BaseItemDto> =
        jellyfinApi.libraryApi
            .getItems(
                userId,
                includeItemTypes = listOf(BaseItemKind.MOVIE),
                recursive = true,
                filters = filters + ItemFilter.IS_UNPLAYED,
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                limit = limit,
            )
            .content
            .items

    private suspend fun download(item: FindroidItem): DownloadResult {
        if (!item.canDownload) return DownloadResult.UNAVAILABLE
        // The lightest version is plenty for a phone screen.
        val source =
            item.sources.filter { it.type == FindroidSourceType.REMOTE }.minByOrNull { it.size }
                ?: return DownloadResult.UNAVAILABLE

        val storageLocation =
            appContext.getExternalFilesDirs(null)[0] ?: return DownloadResult.NO_ROOM
        if (source.size > StatFs(storageLocation.path).availableBytes - queuedBytes) {
            return DownloadResult.NO_ROOM
        }

        val (_, error) = downloader.downloadItem(item, source.id)
        if (error != null) {
            // The server could not be reached or refused the download: the next run will retry.
            Timber.w("Smart download of ${item.name} stopped: $error")
            return DownloadResult.FAILED
        }
        queuedBytes += source.size
        return DownloadResult.QUEUED
    }

    private fun FindroidItem.localSource(): FindroidSource? = sources.firstOrNull {
        it.type == FindroidSourceType.LOCAL
    }

    private enum class DownloadResult {
        QUEUED,
        UNAVAILABLE,
        NO_ROOM,
        FAILED,
    }

    companion object {
        private const val NEXT_UP_LIMIT = 50
        private const val SKIPPED_EPISODES_MARGIN = 10
        private const val PERIODIC_WORK_NAME = "smartDownloads"
        private const val IMMEDIATE_WORK_NAME = "smartDownloadsNow"

        // The periodic and the immediate run may overlap, which would download items twice.
        private val runMutex = Mutex()

        private val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

        fun schedule(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName = PERIODIC_WORK_NAME,
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
                request =
                    PeriodicWorkRequestBuilder<SmartDownloadsWorker>(1, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build(),
            )
            workManager.enqueueUniqueWork(
                uniqueWorkName = IMMEDIATE_WORK_NAME,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                request =
                    OneTimeWorkRequestBuilder<SmartDownloadsWorker>()
                        .setConstraints(constraints)
                        .build(),
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        }
    }
}
