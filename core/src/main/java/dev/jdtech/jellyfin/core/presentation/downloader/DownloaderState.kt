package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager

data class DownloaderState(
    val status: Int = 0,
    val progress: Float = 0f,
) {
    val isDownloading: Boolean
        get() = status in arrayOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING)
}
