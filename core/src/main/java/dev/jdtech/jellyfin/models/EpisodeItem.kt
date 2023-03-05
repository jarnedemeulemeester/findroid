package dev.jdtech.jellyfin.models

import java.util.UUID

sealed class EpisodeItem {
    abstract val id: UUID

    object Header : EpisodeItem() {
        override val id: UUID = UUID.randomUUID()
    }

    data class Episode(val episode: FindroidEpisode) : EpisodeItem() {
        override val id = episode.id
    }
}
