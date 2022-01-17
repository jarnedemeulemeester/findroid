package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.DownloadItem
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.UserItemDataDto
import timber.log.Timber
import java.io.File
import java.util.UUID

var defaultStorage: File? = null

fun requestDownload(
    downloadDatabase: DownloadDatabaseDao,
    uri: Uri,
    downloadRequestItem: DownloadRequestItem,
    context: Context
) {
    val downloadRequest = DownloadManager.Request(uri)
        .setTitle(downloadRequestItem.item.name)
        .setDescription("Downloading")
        .setDestinationUri(
            Uri.fromFile(
                File(
                    defaultStorage,
                    downloadRequestItem.itemId.toString()
                )
            )
        )
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

    try {
        downloadDatabase.insertItem(downloadRequestItem.item)
        if (!File(defaultStorage, downloadRequestItem.itemId.toString()).exists()) {
            val downloadId = downloadFile(downloadRequest, context)
            Timber.d("$downloadId")
            downloadDatabase.updateDownloadId(downloadRequestItem.itemId, downloadId)
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
}

private fun downloadFile(request: DownloadManager.Request, context: Context): Long {
    request.apply {
        setAllowedOverMetered(false)
        setAllowedOverRoaming(false)
    }
    return context.getSystemService<DownloadManager>()!!.enqueue(request)
}

fun getDownloadProgress(context: Context, downloadId: Long): Int {
    val downloadManager = context.getSystemService<DownloadManager>()!!
    val query = DownloadManager.Query().setFilterById(downloadId)
    val cursor = downloadManager.query(query)
    val downloadStatusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
    Timber.d("$downloadStatusIndex")
    if (downloadStatusIndex > 0) {
        Timber.d(cursor.getInt(downloadStatusIndex).toString())
    }
//    val downloadStatus = cursor.getIntOrNull(downloadStatusIndex)!!
//    if (downloadStatus == DownloadManager.STATUS_RUNNING) {
//        val totalBytes = cursor.getLongOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))!!
//        if (totalBytes > 0) {
//            val downloadedBytes = cursor.getLongOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))!!
//            return (downloadedBytes * 100 / totalBytes).toInt()
//        }
//    }
    return -1
}

fun loadDownloadLocation(context: Context) {
    defaultStorage = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
}

fun loadDownloadedEpisodes(downloadDatabase: DownloadDatabaseDao): List<PlayerItem> {
    val items = downloadDatabase.loadItems()
    return items.map {
        PlayerItem(
            name = it.name,
            itemId = it.id,
            mediaSourceId = "",
            playbackPosition = it.playbackPosition ?: 0,
            mediaSourceUri = File(defaultStorage, it.id.toString()).absolutePath,
            item = it
        )
    }
}

fun isItemDownloaded(downloadDatabaseDao: DownloadDatabaseDao, itemId: UUID): Boolean {
    val item = downloadDatabaseDao.loadItem(itemId)
    return item != null
}

fun getDownloadPlayerItem(downloadDatabase: DownloadDatabaseDao, itemId: UUID): PlayerItem? {
    val file = File(defaultStorage!!, itemId.toString())
    try {
        val metadata = downloadDatabase.loadItem(itemId)
        if (metadata != null) {
            return PlayerItem(
                metadata.name,
                UUID.fromString(file.name),
                "",
                metadata.playbackPosition!!,
                file.absolutePath,
                metadata
            )
        }
    } catch (e: Exception) {
        file.delete()
        Timber.e(e)
    }
    return null
}

fun deleteDownloadedEpisode(downloadDatabase: DownloadDatabaseDao, itemId: UUID) {
    try {
        downloadDatabase.deleteItem(itemId)
        File(defaultStorage, itemId.toString()).delete()
    } catch (e: Exception) {
        Timber.e(e)
    }

}

fun postDownloadPlaybackProgress(
    downloadDatabase: DownloadDatabaseDao,
    itemId: UUID,
    playbackPosition: Long,
    playedPercentage: Double
) {
    try {
        downloadDatabase.updatePlaybackPosition(itemId, playbackPosition, playedPercentage)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun downloadMetadataToBaseItemDto(item: DownloadItem): BaseItemDto {
    val userData = UserItemDataDto(
        playbackPositionTicks = item.playbackPosition ?: 0,
        playedPercentage = item.playedPercentage,
        isFavorite = false,
        playCount = 0,
        played = false
    )

    return BaseItemDto(
        id = item.id,
        type = item.type.type,
        seriesName = item.seriesName,
        name = item.name,
        parentIndexNumber = item.parentIndexNumber,
        indexNumber = item.indexNumber,
        userData = userData,
        seriesId = item.seriesId,
        overview = item.overview
    )
}

fun baseItemDtoToDownloadMetadata(item: BaseItemDto): DownloadItem {
    return DownloadItem(
        id = item.id,
        type = item.contentType(),
        name = item.name.orEmpty(),
        played = item.userData?.played ?: false,
        seriesId = item.seriesId,
        seriesName = item.seriesName,
        parentIndexNumber = item.parentIndexNumber,
        indexNumber = item.indexNumber,
        playbackPosition = item.userData?.playbackPositionTicks ?: 0,
        playedPercentage = item.userData?.playedPercentage,
        overview = item.overview
    )
}

suspend fun syncPlaybackProgress(
    downloadDatabase: DownloadDatabaseDao,
    jellyfinRepository: JellyfinRepository
) {
    val items = loadDownloadedEpisodes(downloadDatabase)
    items.forEach {
        try {
            val localPlaybackProgress = it.item?.playbackPosition
            val localPlayedPercentage = it.item?.playedPercentage

            val item = jellyfinRepository.getItem(it.itemId)
            val remotePlaybackProgress = item.userData?.playbackPositionTicks?.div(10000)
            val remotePlayedPercentage = item.userData?.playedPercentage

            var playbackProgress: Long = 0
            var playedPercentage = 0.0

            if (it.item?.played == true || item.userData?.played == true) {
                return@forEach
            }

            if (localPlaybackProgress != null) {
                if (localPlaybackProgress > playbackProgress) {
                    playbackProgress = localPlaybackProgress
                    playedPercentage = localPlayedPercentage!!
                }
            }
            if (remotePlaybackProgress != null) {
                if (remotePlaybackProgress > playbackProgress) {
                    playbackProgress = remotePlaybackProgress
                    playedPercentage = remotePlayedPercentage!!
                }
            }

            if (playbackProgress != 0L) {
                postDownloadPlaybackProgress(
                    downloadDatabase,
                    it.itemId,
                    playbackProgress,
                    playedPercentage
                )
                jellyfinRepository.postPlaybackProgress(
                    it.itemId,
                    playbackProgress.times(10000),
                    true
                )
                Timber.d("Percentage: $playedPercentage")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

    }
}