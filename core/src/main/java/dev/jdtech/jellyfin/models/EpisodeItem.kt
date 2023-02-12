package dev.jdtech.jellyfin.models

import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

sealed class EpisodeItem {
    abstract val id: UUID

    object Header : EpisodeItem() {
        override val id: UUID = UUID.randomUUID()
    }

    data class Episode(val episode: BaseItemDto) : EpisodeItem() {
        override val id = episode.id
    }
}
