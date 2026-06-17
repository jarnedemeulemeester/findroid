package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.FindroidEpisode

sealed interface SeasonDownloaderAction {
    data class Download(val episodes: List<FindroidEpisode>, val storageIndex: Int = 0) :
        SeasonDownloaderAction

    data class DeleteDownload(val episodes: List<FindroidEpisode>) : SeasonDownloaderAction

    data object CancelDownload : SeasonDownloaderAction
}
