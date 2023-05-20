package dev.jdtech.jellyfin.models

import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class FindroidBoxSet(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val played: Boolean = false,
    override val favorite: Boolean = false,
    override val canPlay: Boolean = false,
    override val canDownload: Boolean = false,
    override val sources: List<FindroidSource> = emptyList(),
    override val runtimeTicks: Long = 0L,
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int? = null,
) : FindroidItem

fun BaseItemDto.toFindroidBoxSet(): FindroidBoxSet {
    return FindroidBoxSet(
        id = id,
        name = name.orEmpty(),
    )
}
