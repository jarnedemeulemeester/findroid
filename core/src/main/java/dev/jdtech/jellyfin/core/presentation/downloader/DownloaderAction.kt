package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.offline.download.OfflineProfile

sealed interface DownloaderAction {
    data class Download(
        val item: FindroidItem,
        val profile: OfflineProfile = OfflineProfile.Default480p,
    ) : DownloaderAction

    data class DeleteDownload(val item: FindroidItem) : DownloaderAction

    data class CancelDownload(val item: FindroidItem) : DownloaderAction
}
