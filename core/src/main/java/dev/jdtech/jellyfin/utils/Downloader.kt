package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.UiText

interface Downloader {
    suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int = 0,
    ): Pair<Long, UiText?>

    suspend fun cancelDownload(item: FindroidItem, downloadId: Long)

    suspend fun deleteItem(item: FindroidItem, source: FindroidSource)

    suspend fun getProgress(downloadId: Long?): Pair<Int, Int>

    /** Status, progress, failure reason and bytes in one query rather than several. */
    suspend fun getDownloadInfo(downloadId: Long): DownloadInfo?
}

/**
 * [progress] is 0..100, or -1 when the total size is unknown. [reason] is DownloadManager's
 * COLUMN_REASON, which only means anything for a failed or paused download.
 */
data class DownloadInfo(
    val status: Int,
    val progress: Int,
    val reason: Int = 0,
    val bytesDownloaded: Long = 0,
)
