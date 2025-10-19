package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class JellyCastFolder(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean = false,
    override val canDownload: Boolean = false,
    override val sources: List<JellyCastSource> = emptyList(),
    override val runtimeTicks: Long = 0L,
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int?,
    override val images: JellyCastImages,
    override val chapters: List<JellyCastChapter> = emptyList(),
) : JellyCastItem

fun BaseItemDto.toJellyCastFolder(
    jellyfinRepository: JellyfinRepository,
): JellyCastFolder {
    return JellyCastFolder(
        id = id,
        name = name.orEmpty(),
        played = userData?.played == true,
        favorite = userData?.isFavorite == true,
        unplayedItemCount = userData?.unplayedItemCount,
        images = toJellyCastImages(jellyfinRepository),
    )
}
