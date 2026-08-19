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

    /**
     * Everything DownloadManager knows about one download in a single query, so a queue watching a
     * download does not have to make three.
     */
    suspend fun getDownloadInfo(downloadId: Long): DownloadInfo?
}

/**
 * [progress] is 0..100, or -1 while the total size is still unknown. [reason] is DownloadManager's
 * COLUMN_REASON, which only means anything for a failed or paused download.
 */
data class DownloadInfo(
    val status: Int,
    val progress: Int,
    val reason: Int = 0,
    /** Bytes fetched so far, which is the only progress signal when the total size is unknown. */
    val bytesDownloaded: Long = 0,
)
