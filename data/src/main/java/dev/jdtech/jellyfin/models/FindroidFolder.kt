package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class FindroidFolder(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean = false,
    override val canDownload: Boolean = false,
    override val sources: List<FindroidSource> = emptyList(),
    override val runtimeTicks: Long = 0L,
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int?,
    override val images: FindroidImages,
) : FindroidItem

fun BaseItemDto.toFindroidFolder(
    jellyfinRepository: JellyfinRepository,
): FindroidFolder {
    return FindroidFolder(
        id = id,
        name = name.orEmpty(),
        played = userData?.played ?: false,
        favorite = userData?.isFavorite ?: false,
        unplayedItemCount = userData?.unplayedItemCount,
        images = toFindroidImages(jellyfinRepository),
    )
}
