package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import dev.jdtech.jellyfin.models.UiText

data class DownloaderState(
    val status: Int = 0,
    /**
     * For a batch this is the progress of the item currently downloading, not of the batch. Null
     * means there is no percentage to show: a transcode is generated on the fly and served without
     * a Content-Length, so the total size is unknown for the whole download.
     */
    val progress: Float? = 0f,
    /** Bytes fetched so far. Shown in place of a percentage when [progress] is null. */
    val bytesDownloaded: Long = 0,
    val errorText: UiText? = null,
    /** 1-based position of the item being downloaded; null for a single item download. */
    val itemsCompleted: Int? = null,
    /** Size of the batch; null for a single item download. */
    val itemsTotal: Int? = null,
) {
    /**
     * Really means "keep the downloader card mounted". STATUS_FAILED is in here because the card
     * carries the error text and the retry button, and STATUS_PAUSED because a paused download
     * holds up the whole sequential queue and so has to stay visible and cancellable.
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
