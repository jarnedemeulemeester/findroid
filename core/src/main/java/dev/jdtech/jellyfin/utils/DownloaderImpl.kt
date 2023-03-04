package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.JellyfinItem
import dev.jdtech.jellyfin.models.JellyfinMovieItem
import dev.jdtech.jellyfin.models.JellyfinSource
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSourceDto
import dev.jdtech.jellyfin.repository.JellyfinRepository

class DownloaderImpl(
    context: Context,
    private val jellyfinRepository: JellyfinRepository,
    private val database: ServerDatabaseDao
) : Downloader {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override suspend fun downloadFile(item: JellyfinItem, source: JellyfinSource): Long {
        val path = "findroid/${item.id}.${source.id}"
        when (item) {
            is JellyfinMovieItem -> {
                database.insertMovie(item.toFindroidMovieDto())
                database.insertSource(source.toFindroidSourceDto(item.id, path))
            }
        }

        val uri = jellyfinRepository.getStreamUrl(item.id, item.sources.first { it.id == source.id }.id)
        val request = DownloadManager.Request(uri.toUri())
            .setTitle(item.name)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, path)
        return downloadManager.enqueue(request)
    }
}
