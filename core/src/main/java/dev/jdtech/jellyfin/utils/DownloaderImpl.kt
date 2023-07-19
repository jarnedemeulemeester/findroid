package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.core.net.toUri
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.TrickPlayManifest
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.toFindroidEpisodeDto
import dev.jdtech.jellyfin.models.toFindroidMediaStreamDto
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSeasonDto
import dev.jdtech.jellyfin.models.toFindroidShowDto
import dev.jdtech.jellyfin.models.toFindroidSourceDto
import dev.jdtech.jellyfin.models.toFindroidUserDataDto
import dev.jdtech.jellyfin.models.toIntroDto
import dev.jdtech.jellyfin.models.toTrickPlayManifestDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.io.File
import java.util.UUID
import kotlin.Exception
import dev.jdtech.jellyfin.core.R as CoreR

class DownloaderImpl(
    private val context: Context,
    private val database: ServerDatabaseDao,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : Downloader {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int,
    ): Pair<Long, UiText?> {
        try {
            val source = jellyfinRepository.getMediaSources(item.id, true).first { it.id == sourceId }
            val intro = jellyfinRepository.getIntroTimestamps(item.id)
            val trickPlayManifest = jellyfinRepository.getTrickPlayManifest(item.id)
            val trickPlayData = if (trickPlayManifest != null) {
                jellyfinRepository.getTrickPlayData(
                    item.id,
                    trickPlayManifest.widthResolutions.max(),
                )
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
                is FindroidMovie -> {
                    database.insertMovie(item.toFindroidMovieDto(appPreferences.currentServer!!))
                    database.insertSource(source.toFindroidSourceDto(item.id, path.path.orEmpty()))
                    database.insertUserData(item.toFindroidUserDataDto(jellyfinRepository.getUserId()))
                    downloadExternalMediaStreams(item, source, storageIndex)
                    if (intro != null) {
                        database.insertIntro(intro.toIntroDto(item.id))
                    }
                    if (trickPlayManifest != null && trickPlayData != null) {
                        downloadTrickPlay(item, trickPlayManifest, trickPlayData)
                    }
                    val request = DownloadManager.Request(source.path.toUri())
                        .setTitle(item.name)
                        .setAllowedOverMetered(appPreferences.downloadOverMobileData)
                        .setAllowedOverRoaming(appPreferences.downloadWhenRoaming)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationUri(path)
                    val downloadId = downloadManager.enqueue(request)
                    database.setSourceDownloadId(source.id, downloadId)
                    return Pair(downloadId, null)
                }

                is FindroidEpisode -> {
                    database.insertShow(
                        jellyfinRepository.getShow(item.seriesId)
                            .toFindroidShowDto(appPreferences.currentServer!!),
                    )
                    database.insertSeason(
                        jellyfinRepository.getSeason(item.seasonId).toFindroidSeasonDto(),
                    )
                    database.insertEpisode(item.toFindroidEpisodeDto(appPreferences.currentServer!!))
                    database.insertSource(source.toFindroidSourceDto(item.id, path.path.orEmpty()))
                    database.insertUserData(item.toFindroidUserDataDto(jellyfinRepository.getUserId()))
                    downloadExternalMediaStreams(item, source, storageIndex)
                    if (intro != null) {
                        database.insertIntro(intro.toIntroDto(item.id))
                    }
                    if (trickPlayManifest != null && trickPlayData != null) {
                        downloadTrickPlay(item, trickPlayManifest, trickPlayData)
                    }
                    val request = DownloadManager.Request(source.path.toUri())
                        .setTitle(item.name)
                        .setAllowedOverMetered(appPreferences.downloadOverMobileData)
                        .setAllowedOverRoaming(appPreferences.downloadWhenRoaming)
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
                val source = jellyfinRepository.getMediaSources(item.id).first { it.id == sourceId }
                deleteItem(item, source)
            } catch (_: Exception) {}

            return Pair(-1, if (e.message != null) UiText.DynamicString(e.message!!) else UiText.StringResource(CoreR.string.unknown_error))
        }
    }

    override suspend fun cancelDownload(item: FindroidItem, source: FindroidSource) {
        if (source.downloadId != null) {
            downloadManager.remove(source.downloadId!!)
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
                    val remainingSeasons = database.getSeasonsByShowId(item.seriesId)
                    if (remainingSeasons.isEmpty()) {
                        database.deleteShow(item.seriesId)
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

        database.deleteIntro(item.id)

        database.deleteTrickPlayManifest(item.id)
        File(context.filesDir, "trickplay/${item.id}.bif").delete()
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
        item: FindroidItem,
        source: FindroidSource,
        storageIndex: Int = 0,
    ) {
        val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
        for (mediaStream in source.mediaStreams.filter { it.isExternal }) {
            val id = UUID.randomUUID()
            val streamPath = Uri.fromFile(File(storageLocation, "downloads/${item.id}.${source.id}.$id.download"))
            database.insertMediaStream(mediaStream.toFindroidMediaStreamDto(id, source.id, streamPath.path.orEmpty()))
            val request = DownloadManager.Request(Uri.parse(mediaStream.path))
                .setTitle(mediaStream.title)
                .setAllowedOverMetered(appPreferences.downloadOverMobileData)
                .setAllowedOverRoaming(appPreferences.downloadWhenRoaming)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationUri(streamPath)
            val downloadId = downloadManager.enqueue(request)
            database.setMediaStreamDownloadId(id, downloadId)
        }
    }

    private fun downloadTrickPlay(
        item: FindroidItem,
        trickPlayManifest: TrickPlayManifest,
        byteArray: ByteArray,
    ) {
        database.insertTrickPlayManifest(trickPlayManifest.toTrickPlayManifestDto(item.id))
        File(context.filesDir, "trickplay").mkdirs()
        val file = File(context.filesDir, "trickplay/${item.id}.bif")
        file.writeBytes(byteArray)
    }
}
