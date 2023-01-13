package dev.jdtech.jellyfin.models

import java.util.UUID

sealed class DownloadEpisodeItem {
    abstract val id: UUID

    object Header : DownloadEpisodeItem() {
        override val id: UUID = UUID.randomUUID()
    }

    data class Episode(val episode: PlayerItem) : DownloadEpisodeItem() {
        override val id = episode.itemId
    }
}
