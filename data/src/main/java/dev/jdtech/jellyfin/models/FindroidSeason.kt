package dev.jdtech.jellyfin.models

import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess

data class FindroidSeason(
    override val id: UUID,
    override val name: String,
    val seriesId: UUID,
    val seriesName: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<FindroidSource>,
    val indexNumber: Int,
    val episodes: Collection<FindroidEpisode>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val runtimeTicks: Long = 0L,
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int?,
) : FindroidItem

fun BaseItemDto.toFindroidSeason(): FindroidSeason {
    return FindroidSeason(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        played = userData?.played ?: false,
        favorite = userData?.isFavorite ?: false,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload ?: false,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
        unplayedItemCount = userData?.unplayedItemCount,
        indexNumber = indexNumber ?: 0,
        sources = emptyList(),
        episodes = emptyList(),
        seriesId = seriesId!!,
        seriesName = seriesName.orEmpty(),
    )
}

fun FindroidSeasonDto.toFindroidSeason(): FindroidSeason {
    return FindroidSeason(
        id = id,
        name = name,
        originalTitle = null,
        overview = overview,
        played = played,
        favorite = favorite,
        canPlay = true,
        canDownload = false,
        unplayedItemCount = unplayedItemCount,
        indexNumber = indexNumber,
        sources = emptyList(),
        episodes = emptyList(),
        seriesId = seriesId,
        seriesName = seriesName,
    )
}
