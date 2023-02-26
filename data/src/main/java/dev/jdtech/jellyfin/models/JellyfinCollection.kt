package dev.jdtech.jellyfin.models

import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class JellyfinCollection(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val played: Boolean = false,
    override val favorite: Boolean = false,
    override val canPlay: Boolean = false,
    override val canDownload: Boolean = false,
    override val sources: List<JellyfinSource> = emptyList(),
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int? = null,
    override val playedPercentage: Float? = null,
    val type: CollectionType
) : JellyfinItem

fun BaseItemDto.toJellyfinCollection(): JellyfinCollection {
    return JellyfinCollection(
        id = id,
        name = name.orEmpty(),
        type = CollectionType.values().first { it.type == collectionType },
    )
}
