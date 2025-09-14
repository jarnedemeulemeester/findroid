package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.FindroidItem

sealed interface DownloaderAction {
    data class Download(val item: FindroidItem) : DownloaderAction
    data class DeleteDownload(val item: FindroidItem) : DownloaderAction
    data class CancelDownload(val item: FindroidItem) : DownloaderAction
}
