package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import dev.jdtech.jellyfin.models.UiText

data class DownloaderState(
    val status: Int = 0,
    /** Progress of the item downloading now, or null when the server reported no total size. */
    val progress: Float? = 0f,
    /** Shown in place of a percentage when [progress] is null. */
    val bytesDownloaded: Long = 0,
    val errorText: UiText? = null,
    /** 1-based position within the batch; null when only one item was asked for. */
    val itemsCompleted: Int? = null,
    val itemsTotal: Int? = null,
) {
    /**
     * Really means "keep the downloader card on screen". Failed and paused are in here because the
     * card carries the error and the cancel button, and a paused item holds up everything behind
     * it.
     */
    val isDownloading: Boolean
        get() =
            status in
                arrayOf(
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED,
                    DownloadManager.STATUS_FAILED,
                )
}
