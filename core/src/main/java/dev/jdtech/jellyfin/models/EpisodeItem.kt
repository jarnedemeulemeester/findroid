package dev.jdtech.jellyfin.models

import java.util.UUID

sealed class EpisodeItem {
    abstract val id: UUID

    data class Header(
        val seriesId: UUID,
        val seasonId: UUID,
        val seriesName: String,
        val seasonName: String,
    ) : EpisodeItem() {
        override val id: UUID = UUID.fromString("99abd692-1136-4291-b0b1-11e2bf532cb9")
    }

    data class Episode(val episode: FindroidEpisode) : EpisodeItem() {
        override val id = episode.id
    }
}
