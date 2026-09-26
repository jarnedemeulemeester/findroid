package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
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
import kotlin.Exception
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

class DownloaderImpl(
    private val context: Context,
    private val database: ServerDatabaseDao,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val workManager: WorkManager,
) : Downloader {
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
                val source = jellyfinRepository.getMediaSources(item.id).first { it.id == sourceId }
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
        source.downloadId?.let { cancelWork(it) }
        database.getMediaStreamsBySourceId(source.id).forEach { mediaStream ->
            mediaStream.downloadId?.let { cancelWork(it) }
        }
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

        val mediaStreams = database.getMediaStreamsBySourceId(source.id)
        for (mediaStream in mediaStreams) {
            File(mediaStream.path).delete()
        }
        database.deleteMediaStreamsBySourceId(source.id)

        database.deleteUserData(item.id)

        File(context.filesDir, "trickplay/${item.id}").deleteRecursively()
        File(context.filesDir, "images/${item.id}").deleteRecursively()
    }

    override suspend fun getProgress(downloadId: Long?): Pair<Int, Int> {
        var downloadStatus = -1
        var progress = -1
        if (downloadId == null) {
            return Pair(downloadStatus, progress)
        }
        val workInfo =
            withContext(Dispatchers.IO) {
                workManager
                    .getWorkInfosForUniqueWork(MediaDownloadWorker.uniqueWorkName(downloadId))
                    .get()
                    .lastOrNull()
            }
        when (workInfo?.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                downloadStatus = DownloadManager.STATUS_PENDING
            }
            WorkInfo.State.RUNNING -> {
                downloadStatus = DownloadManager.STATUS_RUNNING
                val totalBytes = workInfo.progress.getLong(MediaDownloadWorker.KEY_TOTAL_BYTES, -1L)
                if (totalBytes > 0) {
                    val downloadedBytes =
                        workInfo.progress.getLong(MediaDownloadWorker.KEY_DOWNLOADED_BYTES, 0L)
                    progress = downloadedBytes.times(100).div(totalBytes).toInt()
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                downloadStatus = DownloadManager.STATUS_SUCCESSFUL
                progress = 100
            }
            else -> {
                downloadStatus = DownloadManager.STATUS_FAILED
            }
        }
        return Pair(downloadStatus, progress)
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
    ) {
        val networkType =
            when {
                !appPreferences.getValue(appPreferences.downloadOverMobileData) ->
                    NetworkType.UNMETERED
                !appPreferences.getValue(appPreferences.downloadWhenRoaming) ->
                    NetworkType.NOT_ROAMING
                else -> NetworkType.CONNECTED
            }

        val request =
            OneTimeWorkRequestBuilder<MediaDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
                .setInputData(
                    workDataOf(
                        MediaDownloadWorker.KEY_URL to url,
                        MediaDownloadWorker.KEY_PATH to path,
                        MediaDownloadWorker.KEY_TITLE to title,
                        MediaDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                        MediaDownloadWorker.KEY_SHOW_NOTIFICATION to showNotification,
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
    }

    private fun startImagesDownloader(item: FindroidItem) {
        val downloadImagesRequest =
            OneTimeWorkRequestBuilder<ImagesDownloaderWorker>()
                .setInputData(workDataOf(ImagesDownloaderWorker.KEY_ITEM_ID to item.id.toString()))
                .build()

        workManager.enqueue(downloadImagesRequest)
    }
}
