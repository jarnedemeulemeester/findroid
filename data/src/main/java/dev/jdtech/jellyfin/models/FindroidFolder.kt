package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess
import java.util.UUID

data class FindroidFolder(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<FindroidSource>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
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
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        played = userData?.played ?: false,
        favorite = userData?.isFavorite ?: false,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload ?: false,
        unplayedItemCount = userData?.unplayedItemCount,
        sources = emptyList(),
        images = toFindroidImages(jellyfinRepository),
    )
}
