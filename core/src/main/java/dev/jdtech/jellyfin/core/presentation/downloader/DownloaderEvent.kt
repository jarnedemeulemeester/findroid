package dev.jdtech.jellyfin.core.presentation.downloader

import android.content.Intent

sealed interface DownloaderEvent {
    data object Successful : DownloaderEvent

    data object Deleted : DownloaderEvent

    data class BatchQueued(val episodeCount: Int) : DownloaderEvent

    data class StoragePermissionRequired(val intent: Intent, val fallbackIntent: Intent) :
        DownloaderEvent
}
