package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.core.net.toUri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastSource
import dev.jdtech.jellyfin.models.JellyCastSources
import dev.jdtech.jellyfin.models.JellyCastTrickplayInfo
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.toJellyCastEpisodeDto
import dev.jdtech.jellyfin.models.toJellyCastMediaStreamDto
import dev.jdtech.jellyfin.models.toJellyCastMovieDto
import dev.jdtech.jellyfin.models.toJellyCastSeasonDto
import dev.jdtech.jellyfin.models.toJellyCastSegmentsDto
import dev.jdtech.jellyfin.models.toJellyCastShowDto
import dev.jdtech.jellyfin.models.toJellyCastSourceDto
import dev.jdtech.jellyfin.models.toJellyCastTrickplayInfoDto
import dev.jdtech.jellyfin.models.toJellyCastUserDataDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import timber.log.Timber
import java.io.File
import java.util.UUID
import kotlin.Exception
import kotlin.math.ceil
import dev.jdtech.jellyfin.core.R as CoreR

class DownloaderImpl(
    private val context: Context,
    private val database: ServerDatabaseDao,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : Downloader {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override suspend fun downloadItem(
        item: JellyCastItem,
        sourceId: String,
        storageIndex: Int,
    ): Pair<Long, UiText?> {
        try {
            val sources = jellyfinRepository.getMediaSources(item.id, true)
            val source = sources.firstOrNull { it.id == sourceId } ?: sources.firstOrNull()
                ?: return Pair(-1, UiText.StringResource(CoreR.string.source_unavailable))
            val segments = jellyfinRepository.getSegments(item.id)
            val trickplayInfo = if (item is JellyCastSources) {
                item.trickplayInfo?.get(sourceId)
            } else {
                null
            }
            val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
            if (storageLocation == null || Environment.getExternalStorageState(storageLocation) != Environment.MEDIA_MOUNTED) {
                return Pair(-1, UiText.StringResource(CoreR.string.storage_unavailable))
            }
            val path =
                Uri.fromFile(File(storageLocation, "downloads/${item.id}.${source.id}.download"))
            val stats = StatFs(storageLocation.path)
            if (stats.availableBytes < source.size) {
                return Pair(
                    -1,
                    UiText.StringResource(
                        CoreR.string.not_enough_storage,
                        Formatter.formatFileSize(context, source.size),
                        Formatter.formatFileSize(context, stats.availableBytes),
                    ),
                )
            }
            when (item) {
                is JellyCastMovie -> {
                    database.insertMovie(item.toJellyCastMovieDto(appPreferences.getValue(appPreferences.currentServer)))
                    database.insertSource(source.toJellyCastSourceDto(item.id, path.path.orEmpty()))
                    database.insertUserData(item.toJellyCastUserDataDto(jellyfinRepository.getUserId()))
                    downloadExternalMediaStreams(item, source, storageIndex)
                    segments.forEach {
                        database.insertSegment(it.toJellyCastSegmentsDto(item.id))
                    }
                    if (trickplayInfo != null) {
                        downloadTrickplayData(item.id, sourceId, trickplayInfo)
                    }
                    val request = DownloadManager.Request(source.path.toUri())
                        .setTitle(item.name)
                        .setAllowedOverMetered(appPreferences.getValue(appPreferences.downloadOverMobileData))
                        .setAllowedOverRoaming(appPreferences.getValue(appPreferences.downloadWhenRoaming))
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationUri(path)
                    val downloadId = downloadManager.enqueue(request)
                    database.setSourceDownloadId(source.id, downloadId)
                    return Pair(downloadId, null)
                }

                is JellyCastEpisode -> {
                    database.insertShow(
                        jellyfinRepository.getShow(item.seriesId)
                            .toJellyCastShowDto(appPreferences.getValue(appPreferences.currentServer)),
                    )
                    database.insertSeason(
                        jellyfinRepository.getSeason(item.seasonId).toJellyCastSeasonDto(),
                    )
                    database.insertEpisode(item.toJellyCastEpisodeDto(appPreferences.getValue(appPreferences.currentServer)))
                    database.insertSource(source.toJellyCastSourceDto(item.id, path.path.orEmpty()))
                    database.insertUserData(item.toJellyCastUserDataDto(jellyfinRepository.getUserId()))
                    downloadExternalMediaStreams(item, source, storageIndex)
                    segments.forEach {
                        database.insertSegment(it.toJellyCastSegmentsDto(item.id))
                    }
                    if (trickplayInfo != null) {
                        downloadTrickplayData(item.id, sourceId, trickplayInfo)
                    }
                    val request = DownloadManager.Request(source.path.toUri())
                        .setTitle(item.name)
                        .setAllowedOverMetered(appPreferences.getValue(appPreferences.downloadOverMobileData))
                        .setAllowedOverRoaming(appPreferences.getValue(appPreferences.downloadWhenRoaming))
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationUri(path)
                    val downloadId = downloadManager.enqueue(request)
                    database.setSourceDownloadId(source.id, downloadId)
                    return Pair(downloadId, null)
                }
            }
            return Pair(-1, null)
        } catch (e: Exception) {
            try {
                val cleanupSource = jellyfinRepository.getMediaSources(item.id).firstOrNull { it.id == sourceId }
                    ?: jellyfinRepository.getMediaSources(item.id).firstOrNull()
                if (cleanupSource != null) {
                    deleteItem(item, cleanupSource)
                }
            } catch (_: Exception) {}

            return Pair(-1, if (e.message != null) UiText.DynamicString(e.message!!) else UiText.StringResource(CoreR.string.unknown_error))
        }
    }

    override suspend fun cancelDownload(item: JellyCastItem, source: JellyCastSource) {
        if (source.downloadId != null) {
            downloadManager.remove(source.downloadId!!)
        }
        // Clean up the partial download but keep item metadata
        deleteItem(item, source)
    }

    override suspend fun deleteItem(item: JellyCastItem, source: JellyCastSource) {
        // Delete the downloaded source and files, but keep the item metadata in DB
        // so it can be re-downloaded later and still appears in the library
        Timber.tag("Downloader").d("deleteItem: removing source %s for item %s (type=%s)", source.id, item.id, item::class.simpleName)
        database.deleteSource(source.id)
        File(source.path).delete()

        val mediaStreams = database.getMediaStreamsBySourceId(source.id)
        for (mediaStream in mediaStreams) {
            File(mediaStream.path).delete()
        }
        database.deleteMediaStreamsBySourceId(source.id)

        File(context.filesDir, "trickplay/${item.id}").deleteRecursively()
        Timber.tag("Downloader").d("deleteItem: completed for item %s (item metadata preserved in DB)", item.id)
    }

    override suspend fun getProgress(downloadId: Long?): Pair<Int, Int> {
        var downloadStatus = -1
        var progress = -1
        if (downloadId == null) {
            return Pair(downloadStatus, progress)
        }
        val query = DownloadManager.Query()
            .setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            downloadStatus = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_STATUS,
                ),
            )
            when (downloadStatus) {
                DownloadManager.STATUS_RUNNING -> {
                    val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (totalBytes > 0) {
                        val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        progress = downloadedBytes.times(100).div(totalBytes).toInt()
                    }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 100
                }
            }
        } else {
            downloadStatus = DownloadManager.STATUS_FAILED
        }
        return Pair(downloadStatus, progress)
    }

    private fun downloadExternalMediaStreams(
        item: JellyCastItem,
        source: JellyCastSource,
        storageIndex: Int = 0,
    ) {
        val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
        for (mediaStream in source.mediaStreams.filter { it.isExternal }) {
            val id = UUID.randomUUID()
            val streamPath = Uri.fromFile(File(storageLocation, "downloads/${item.id}.${source.id}.$id.download"))
            database.insertMediaStream(mediaStream.toJellyCastMediaStreamDto(id, source.id, streamPath.path.orEmpty()))
            val request = DownloadManager.Request(mediaStream.path!!.toUri())
                .setTitle(mediaStream.title)
                .setAllowedOverMetered(appPreferences.getValue(appPreferences.downloadOverMobileData))
                .setAllowedOverRoaming(appPreferences.getValue(appPreferences.downloadWhenRoaming))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationUri(streamPath)
            val downloadId = downloadManager.enqueue(request)
            database.setMediaStreamDownloadId(id, downloadId)
        }
    }

    private suspend fun downloadTrickplayData(
        itemId: UUID,
        sourceId: String,
        trickplayInfo: JellyCastTrickplayInfo,
    ) {
        val maxIndex = ceil(trickplayInfo.thumbnailCount.toDouble().div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)).toInt()
        val byteArrays = mutableListOf<ByteArray>()
        for (i in 0..maxIndex) {
            jellyfinRepository.getTrickplayData(
                itemId,
                trickplayInfo.width,
                i,
            )?.let { byteArray ->
                byteArrays.add(byteArray)
            }
        }
        saveTrickplayData(itemId, sourceId, trickplayInfo, byteArrays)
    }

    private fun saveTrickplayData(
        itemId: UUID,
        sourceId: String,
        trickplayInfo: JellyCastTrickplayInfo,
        byteArrays: List<ByteArray>,
    ) {
        val basePath = "trickplay/$itemId/$sourceId"
        database.insertTrickplayInfo(trickplayInfo.toJellyCastTrickplayInfoDto(sourceId))
        File(context.filesDir, basePath).mkdirs()
        for ((i, byteArray) in byteArrays.withIndex()) {
            val file = File(context.filesDir, "$basePath/$i")
            file.writeBytes(byteArray)
        }
    }
}
