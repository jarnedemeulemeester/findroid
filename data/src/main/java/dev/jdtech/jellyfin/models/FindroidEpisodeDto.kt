package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "episodes")
data class FindroidEpisodeDto(
    @PrimaryKey
    val id: UUID,
    val serverId: String?,
    val seasonId: UUID,
    val seriesId: UUID,
    val name: String,
    val seriesName: String,
    val overview: String,
    val indexNumber: Int,
    val indexNumberEnd: Int,
    val parentIndexNumber: Int,
    val played: Boolean,
    val favorite: Boolean,
    val runtimeTicks: Long,
    val playbackPositionTicks: Long,
    val premiereDate: LocalDateTime?,
    val communityRating: Float?,
)

fun FindroidEpisode.toFindroidEpisodeDto(serverId: String? = null): FindroidEpisodeDto {
    return FindroidEpisodeDto(
        id = id,
        serverId = serverId,
        seasonId = seasonId,
        seriesId = seriesId,
        name = name,
        seriesName = seriesName,
        overview = overview,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
        parentIndexNumber = parentIndexNumber,
        played = played,
        favorite = favorite,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = playbackPositionTicks,
        premiereDate = premiereDate,
        communityRating = communityRating,
    )
}
