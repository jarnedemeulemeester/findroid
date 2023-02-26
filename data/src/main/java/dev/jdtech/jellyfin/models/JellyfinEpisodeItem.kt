package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.PlayAccess

data class JellyfinEpisodeItem(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    val indexNumber: Int,
    val indexNumberEnd: Int,
    val parentIndexNumber: Int,
    override val sources: List<JellyfinSource>,
    override val playedPercentage: Float? = null,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val runtimeTicks: Long,
    override val playbackPositionTicks: Long,
    val premiereDate: DateTime?,
    val seriesName: String,
    val seriesId: UUID,
    val seasonId: UUID,
    val communityRating: Float?,
    override val unplayedItemCount: Int? = null,
) : JellyfinItem, JellyfinSources

suspend fun BaseItemDto.toJellyfinEpisodeItem(jellyfinRepository: JellyfinRepository? = null): JellyfinEpisodeItem {
    return JellyfinEpisodeItem(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        indexNumber = indexNumber ?: 0,
        indexNumberEnd = indexNumberEnd ?: 0,
        parentIndexNumber = parentIndexNumber ?: 0,
        sources = mediaSources?.map { it.toJellyfinSource(jellyfinRepository, id) } ?: emptyList(),
        played = userData?.played ?: false,
        favorite = userData?.isFavorite ?: false,
        playedPercentage = userData?.playedPercentage?.toFloat(),
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload ?: false,
        runtimeTicks = runTimeTicks ?: 0,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
        premiereDate = premiereDate,
        seriesName = seriesName.orEmpty(),
        seriesId = seriesId!!,
        seasonId = seasonId!!,
        communityRating = communityRating,
    )
}

fun JellyfinEpisodeItem.toBaseItemDto(): BaseItemDto {
    return BaseItemDto(
        id = this.id,
        name = this.name,
        seriesId = this.seriesId,
        seriesName = this.seriesName,
        type = BaseItemKind.EPISODE,
    )
}
