package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.JellyfinItem
import dev.jdtech.jellyfin.models.JellyfinMovieItem
import dev.jdtech.jellyfin.models.JellyfinSource
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSourceDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.io.File

class DownloaderImpl(
    private val context: Context,
    private val jellyfinRepository: JellyfinRepository,
    private val database: ServerDatabaseDao
) : Downloader {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override suspend fun downloadItem(item: JellyfinItem, source: JellyfinSource): Long {
        val path = Uri.fromFile(File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "${item.id}.${source.id}.download"))
        when (item) {
            is JellyfinMovieItem -> {
                database.insertMovie(item.toFindroidMovieDto())
                database.insertSource(source.toFindroidSourceDto(item.id, path.path.orEmpty()))
            }
        }

        val uri = jellyfinRepository.getStreamUrl(item.id, item.sources.first { it.id == source.id }.id)
        val request = DownloadManager.Request(uri.toUri())
            .setTitle(item.name)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(path)
        val downloadId = downloadManager.enqueue(request)
        database.setSourceDownloadId(source.id, downloadId)
        return downloadId
    }

    override suspend fun deleteItem(item: JellyfinItem, source: JellyfinSource) {
        database.deleteMovie(item.id)
        database.deleteSource(source.id)
        File(source.path).delete()
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
