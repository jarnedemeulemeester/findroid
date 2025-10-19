package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class JellyCastBoxSet(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val played: Boolean = false,
    override val favorite: Boolean = false,
    override val canPlay: Boolean = false,
    override val canDownload: Boolean = false,
    override val sources: List<JellyCastSource> = emptyList(),
    override val runtimeTicks: Long = 0L,
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int? = null,
    override val images: JellyCastImages,
    override val chapters: List<JellyCastChapter> = emptyList(),
) : JellyCastItem

fun BaseItemDto.toJellyCastBoxSet(
    jellyfinRepository: JellyfinRepository,
): JellyCastBoxSet {
    return JellyCastBoxSet(
        id = id,
        name = name.orEmpty(),
        images = toJellyCastImages(jellyfinRepository),
    )
}
