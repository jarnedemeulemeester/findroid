package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.toFindroidMediaStreamDto
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSourceDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.io.File
import java.util.UUID

class DownloaderImpl(
    private val context: Context,
    private val jellyfinRepository: JellyfinRepository,
    private val database: ServerDatabaseDao
) : Downloader {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override suspend fun downloadItem(
        item: FindroidItem,
        source: FindroidSource,
        serverId: String,
        baseUrl: String
    ): Long {
        val path = Uri.fromFile(File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "${item.id}.${source.id}.download"))
        when (item) {
            is FindroidMovie -> {
                database.insertMovie(item.toFindroidMovieDto(serverId))
                database.insertSource(source.toFindroidSourceDto(item.id, path.path.orEmpty()))
                for (mediaStream in source.mediaStreams.filter { it.isExternal }) {
                    val id = UUID.randomUUID()
                    val streamPath = Uri.fromFile(File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "$id.download"))
                    database.insertMediaStream(mediaStream.toFindroidMediaStreamDto(id, source.id, streamPath.path.orEmpty()))
                    val request = DownloadManager.Request(Uri.parse(baseUrl + mediaStream.path))
                        .setTitle(mediaStream.title)
                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationUri(streamPath)
                    val downloadId = downloadManager.enqueue(request)
                    database.setMediaStreamDownloadId(id, downloadId)
                }
            }
        }

        val uri = jellyfinRepository.getStreamUrl(item.id, source.id)
        val request = DownloadManager.Request(uri.toUri())
            .setTitle(item.name)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(path)
        val downloadId = downloadManager.enqueue(request)
        database.setSourceDownloadId(source.id, downloadId)
        return downloadId
    }

    override suspend fun deleteItem(item: FindroidItem, source: FindroidSource) {
        database.deleteMovie(item.id)

        database.deleteSource(source.id)
        File(source.path).delete()

        val mediaStreams = database.getMediaStreamsBySourceId(source.id)
        for (mediaStream in mediaStreams) {
            File(mediaStream.path).delete()
        }
        database.deleteMediaStreamsBySourceId(source.id)
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
                    DownloadManager.COLUMN_STATUS
                )
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
        }
        return Pair(downloadStatus, progress)
    }
}
