package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSources
import dev.jdtech.jellyfin.models.FindroidTrickplayInfo
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.toFindroidEpisodeDto
import dev.jdtech.jellyfin.models.toFindroidMediaStreamDto
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSeasonDto
import dev.jdtech.jellyfin.models.toFindroidSegmentsDto
import dev.jdtech.jellyfin.models.toFindroidShowDto
import dev.jdtech.jellyfin.models.toFindroidSource
import dev.jdtech.jellyfin.models.toFindroidSourceDto
import dev.jdtech.jellyfin.models.toFindroidTrickplayInfoDto
import dev.jdtech.jellyfin.models.toFindroidUserDataDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.work.ImagesDownloaderWorker
import dev.jdtech.jellyfin.work.MediaDownloadWorker
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.Exception
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class DownloaderImpl(
    private val context: Context,
    private val database: ServerDatabaseDao,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val workManager: WorkManager,
) : Downloader {
    // Only used for downloads which were started before downloading moved into the app
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    // TODO: We should probably move most (if not all) code to a worker.
    //  At this moment it is possible that some things are not downloaded due to the user leaving
    //  the current screen
    override suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int,
    ): Pair<Long, UiText?> = coroutineScope {
        try {
            val source =
                jellyfinRepository.getMediaSources(item.id, true).first { it.id == sourceId }
            val segments = jellyfinRepository.getSegments(item.id)
            val trickplayInfo =
                if (item is FindroidSources) {
                    item.trickplayInfo?.get(sourceId)
                } else {
                    null
                }
            val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
            if (
                storageLocation == null ||
                    Environment.getExternalStorageState(storageLocation) !=
                        Environment.MEDIA_MOUNTED
            ) {
                return@coroutineScope Pair(
                    -1,
                    UiText.StringResource(CoreR.string.storage_unavailable),
                )
            }
            val path =
                Uri.fromFile(File(storageLocation, "downloads/${item.id}.${source.id}.download"))
            val stats = StatFs(storageLocation.path)
            if (stats.availableBytes < source.size) {
                return@coroutineScope Pair(
                    -1,
                    UiText.StringResource(
                        CoreR.string.not_enough_storage,
                        Formatter.formatFileSize(context, source.size),
                        Formatter.formatFileSize(context, stats.availableBytes),
                    ),
                )
            }
            // Retrying a failed download: stop and remove what is left of the previous attempt
            database
                .getSources(item.id)
                .firstOrNull { it.id == sourceId }
                ?.let { previous ->
                    cancelWorks(previous.id, previous.downloadId)
                    deleteMediaStreams(previous.id)
                }
            val downloadId = newDownloadId()

            when (item) {
                is FindroidMovie -> {
                    database.insertMovie(
                        item.toFindroidMovieDto(
                            appPreferences.getValue(appPreferences.currentServer)
                        )
                    )
                }
                is FindroidEpisode -> {
                    val show = jellyfinRepository.getShow(item.seriesId)
                    database.insertShow(
                        show.toFindroidShowDto(
                            appPreferences.getValue(appPreferences.currentServer)
                        )
                    )
                    val season = jellyfinRepository.getSeason(item.seasonId)
                    database.insertSeason(season.toFindroidSeasonDto())
                    database.insertEpisode(
                        item.toFindroidEpisodeDto(
                            appPreferences.getValue(appPreferences.currentServer)
                        )
                    )

                    startImagesDownloader(show)
                    startImagesDownloader(season)
                }
            }

            val sourceDto = source.toFindroidSourceDto(item.id, path.path.orEmpty())

            database.insertSource(sourceDto.copy(downloadId = downloadId))
            enqueueDownload(
                downloadId = downloadId,
                url = source.path,
                path = path.path.orEmpty(),
                title = item.name,
                showNotification = true,
                item = item,
            )
            database.insertUserData(item.toFindroidUserDataDto(jellyfinRepository.getUserId()))

            downloadExternalMediaStreams(item, source, storageIndex)

            segments.forEach { database.insertSegment(it.toFindroidSegmentsDto(item.id)) }

            if (trickplayInfo != null) {
                downloadTrickplayData(item.id, sourceId, trickplayInfo)
            }

            startImagesDownloader(item)
            return@coroutineScope Pair(downloadId, null)
        } catch (e: Exception) {
            try {
                // Prefer the local source, it knows which workers and files have to be cleaned up
                val source =
                    database
                        .getSources(item.id)
                        .firstOrNull { it.id == sourceId }
                        ?.toFindroidSource(database)
                        ?: jellyfinRepository.getMediaSources(item.id).first { it.id == sourceId }
                cancelWorks(source.id, source.downloadId)
                deleteItem(item, source)
            } catch (_: Exception) {}
            Timber.e(e)
            return@coroutineScope Pair(
                -1,
                if (e.message != null) UiText.DynamicString(e.message!!)
                else UiText.StringResource(CoreR.string.unknown_error),
            )
        }
    }

    override suspend fun cancelDownload(item: FindroidItem, downloadId: Long) {
        val source =
            database.getSourceByDownloadId(downloadId)?.toFindroidSource(database) ?: return
        cancelWorks(source.id, source.downloadId)
        deleteItem(item, source)
    }

    override suspend fun deleteItem(item: FindroidItem, source: FindroidSource) {
        when (item) {
            is FindroidMovie -> {
                database.deleteMovie(item.id)
            }
            is FindroidEpisode -> {
                database.deleteEpisode(item.id)
                val remainingEpisodes = database.getEpisodesBySeasonId(item.seasonId)
                if (remainingEpisodes.isEmpty()) {
                    database.deleteSeason(item.seasonId)
                    database.deleteUserData(item.seasonId)
                    File(context.filesDir, "trickplay/${item.seasonId}").deleteRecursively()
                    File(context.filesDir, "images/${item.seasonId}").deleteRecursively()
                    val remainingSeasons = database.getSeasonsByShowId(item.seriesId)
                    if (remainingSeasons.isEmpty()) {
                        database.deleteShow(item.seriesId)
                        database.deleteUserData(item.seriesId)
                        File(context.filesDir, "trickplay/${item.seriesId}").deleteRecursively()
                        File(context.filesDir, "images/${item.seriesId}").deleteRecursively()
                    }
                }
            }
        }

        database.deleteSource(source.id)
        File(source.path).delete()

        deleteMediaStreams(source.id)

        database.deleteUserData(item.id)

        File(context.filesDir, "trickplay/${item.id}").deleteRecursively()
        File(context.filesDir, "images/${item.id}").deleteRecursively()
    }

    override suspend fun getProgress(downloadId: Long?): Pair<Int, Int> {
        if (downloadId == null) {
            return Pair(-1, -1)
        }
        val workInfo =
            workManager
                .getWorkInfosForUniqueWorkFlow(MediaDownloadWorker.uniqueWorkName(downloadId))
                .first()
                .lastOrNull() ?: return getLegacyProgress(downloadId)

        return when (workInfo.state) {
            WorkInfo.State.BLOCKED -> Pair(DownloadManager.STATUS_PENDING, -1)
            WorkInfo.State.ENQUEUED -> {
                // Waiting for the network or to retry after a failed or interrupted attempt
                if (workInfo.runAttemptCount > 0) {
                    Pair(DownloadManager.STATUS_PAUSED, -1)
                } else {
                    Pair(DownloadManager.STATUS_PENDING, -1)
                }
            }
            WorkInfo.State.RUNNING -> {
                var progress = -1
                val totalBytes = workInfo.progress.getLong(MediaDownloadWorker.KEY_TOTAL_BYTES, -1L)
                if (totalBytes > 0) {
                    val downloadedBytes =
                        workInfo.progress.getLong(MediaDownloadWorker.KEY_DOWNLOADED_BYTES, 0L)
                    progress = downloadedBytes.times(100).div(totalBytes).toInt()
                }
                Pair(DownloadManager.STATUS_RUNNING, progress)
            }
            WorkInfo.State.SUCCEEDED -> Pair(DownloadManager.STATUS_SUCCESSFUL, 100)
            // The item is still marked as downloading but nothing will finish it, offer a retry
            WorkInfo.State.FAILED,
            WorkInfo.State.CANCELLED -> Pair(DownloadManager.STATUS_FAILED, -1)
        }
    }

    /**
     * Downloads which were started by a previous version of the app are still handled by the system
     * DownloadManager. Report their progress and finish them once they are complete.
     */
    private suspend fun getLegacyProgress(downloadId: Long): Pair<Int, Int> {
        val (status, progress) = queryDownloadManager(downloadId)
        if (status != DownloadManager.STATUS_SUCCESSFUL) {
            return Pair(status, progress)
        }

        val source =
            database.getSourceByDownloadId(downloadId)
                ?: return Pair(DownloadManager.STATUS_FAILED, -1)
        val path = source.path.removeSuffix(".download")
        if (!File(source.path).renameTo(File(path))) {
            return Pair(DownloadManager.STATUS_FAILED, -1)
        }
        database.setSourcePath(source.id, path)

        for (mediaStream in database.getMediaStreamsBySourceId(source.id)) {
            if (!mediaStream.path.endsWith(".download")) continue
            val streamPath = mediaStream.path.removeSuffix(".download")
            val finished =
                mediaStream.downloadId?.let { queryDownloadManager(it).first } ==
                    DownloadManager.STATUS_SUCCESSFUL
            if (finished && File(mediaStream.path).renameTo(File(streamPath))) {
                database.setMediaStreamPath(mediaStream.id, streamPath)
            } else {
                mediaStream.downloadId?.let { cancelWork(it) }
                File(mediaStream.path).delete()
                database.deleteMediaStream(mediaStream.id)
            }
        }
        return Pair(DownloadManager.STATUS_SUCCESSFUL, 100)
    }

    private suspend fun queryDownloadManager(downloadId: Long): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            var downloadStatus = DownloadManager.STATUS_FAILED
            var progress = -1
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor
                ->
                if (cursor.moveToFirst()) {
                    downloadStatus =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val totalBytes =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                    if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                        progress = 100
                    } else if (totalBytes > 0) {
                        val downloadedBytes =
                            cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                )
                            )
                        progress = downloadedBytes.times(100).div(totalBytes).toInt()
                    }
                }
            }
            Pair(downloadStatus, progress)
        }

    private suspend fun downloadExternalMediaStreams(
        item: FindroidItem,
        source: FindroidSource,
        storageIndex: Int = 0,
    ) {
        val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
        for (mediaStream in source.mediaStreams.filter { it.isExternal }) {
            val id = UUID.randomUUID()
            val streamPath =
                Uri.fromFile(
                    File(storageLocation, "downloads/${item.id}.${source.id}.$id.download")
                )
            val downloadId = newDownloadId()
            database.insertMediaStream(
                mediaStream
                    .toFindroidMediaStreamDto(id, source.id, streamPath.path.orEmpty())
                    .copy(downloadId = downloadId)
            )
            enqueueDownload(
                downloadId = downloadId,
                url = mediaStream.path!!,
                path = streamPath.path.orEmpty(),
                title = mediaStream.title,
                showNotification = false,
                item = null,
            )
        }
    }

    private suspend fun downloadTrickplayData(
        itemId: UUID,
        sourceId: String,
        trickplayInfo: FindroidTrickplayInfo,
    ) {
        val maxIndex =
            ceil(
                    trickplayInfo.thumbnailCount
                        .toDouble()
                        .div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)
                )
                .toInt()
        val byteArrays = mutableListOf<ByteArray>()
        for (i in 0..maxIndex) {
            jellyfinRepository.getTrickplayData(itemId, trickplayInfo.width, i)?.let { byteArray ->
                byteArrays.add(byteArray)
            }
        }
        saveTrickplayData(itemId, sourceId, trickplayInfo, byteArrays)
    }

    private suspend fun saveTrickplayData(
        itemId: UUID,
        sourceId: String,
        trickplayInfo: FindroidTrickplayInfo,
        byteArrays: List<ByteArray>,
    ) {
        val basePath = "trickplay/$itemId/$sourceId"
        database.insertTrickplayInfo(trickplayInfo.toFindroidTrickplayInfoDto(sourceId))
        File(context.filesDir, basePath).mkdirs()
        for ((i, byteArray) in byteArrays.withIndex()) {
            val file = File(context.filesDir, "$basePath/$i")
            file.writeBytes(byteArray)
        }
    }

    /** Positive random id, so it never collides with the -1 error value. */
    private fun newDownloadId(): Long = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

    private fun enqueueDownload(
        downloadId: Long,
        url: String,
        path: String,
        title: String,
        showNotification: Boolean,
        item: FindroidItem?,
    ) {
        val networkType =
            when {
                !appPreferences.getValue(appPreferences.downloadOverMobileData) ->
                    NetworkType.UNMETERED
                !appPreferences.getValue(appPreferences.downloadWhenRoaming) ->
                    NetworkType.NOT_ROAMING
                else -> NetworkType.CONNECTED
            }

        val itemKind =
            when (item) {
                is FindroidMovie -> MediaDownloadWorker.ITEM_KIND_MOVIE
                is FindroidEpisode -> MediaDownloadWorker.ITEM_KIND_EPISODE
                else -> null
            }

        val request =
            OneTimeWorkRequestBuilder<MediaDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
                // The worker resumes where it left off, so there is no reason to wait long
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .setInputData(
                    workDataOf(
                        MediaDownloadWorker.KEY_URL to url,
                        MediaDownloadWorker.KEY_PATH to path,
                        MediaDownloadWorker.KEY_TITLE to title,
                        MediaDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                        MediaDownloadWorker.KEY_SHOW_NOTIFICATION to showNotification,
                        MediaDownloadWorker.KEY_ITEM_ID to item?.id?.toString(),
                        MediaDownloadWorker.KEY_ITEM_KIND to itemKind,
                    )
                )
                .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName = MediaDownloadWorker.uniqueWorkName(downloadId),
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request,
        )
    }

    private fun cancelWork(downloadId: Long) {
        workManager.cancelUniqueWork(MediaDownloadWorker.uniqueWorkName(downloadId))
        // A notification about how the download ended is of no use anymore
        NotificationManagerCompat.from(context)
            .cancel(MediaDownloadWorker.finishedNotificationId(downloadId))
        // No-op unless it is a download from before downloading moved into the app
        downloadManager.remove(downloadId)
    }

    /** Stop downloading a source and its external media streams. */
    private suspend fun cancelWorks(sourceId: String, downloadId: Long?) {
        downloadId?.let { cancelWork(it) }
        database.getMediaStreamsBySourceId(sourceId).forEach { mediaStream ->
            mediaStream.downloadId?.let { cancelWork(it) }
        }
    }

    /**
     * The rows are removed before the files, a worker which is still stopping uses that to notice
     * its file is no longer wanted.
     */
    private suspend fun deleteMediaStreams(sourceId: String) {
        val mediaStreams = database.getMediaStreamsBySourceId(sourceId)
        database.deleteMediaStreamsBySourceId(sourceId)
        for (mediaStream in mediaStreams) {
            File(mediaStream.path).delete()
        }
    }

    private fun startImagesDownloader(item: FindroidItem) {
        val downloadImagesRequest =
            OneTimeWorkRequestBuilder<ImagesDownloaderWorker>()
                .setInputData(workDataOf(ImagesDownloaderWorker.KEY_ITEM_ID to item.id.toString()))
                .build()

        workManager.enqueue(downloadImagesRequest)
    }
}
