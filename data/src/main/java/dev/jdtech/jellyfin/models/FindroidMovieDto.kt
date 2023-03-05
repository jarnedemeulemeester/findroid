package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import org.jellyfin.sdk.model.DateTime

@Entity(tableName = "movies")
data class FindroidMovieDto(
    @PrimaryKey
    val id: UUID,
    val serverId: String?,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val played: Boolean,
    val favorite: Boolean,
    val runtimeTicks: Long,
    val playbackPositionTicks: Long,
    val premiereDate: DateTime?,
//    val genres: List<String>,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
    val endDate: DateTime?,
    val unplayedItemCount: Int? = null,
)

fun FindroidMovie.toFindroidMovieDto(serverId: String? = null): FindroidMovieDto {
    return FindroidMovieDto(
        id = id,
        serverId = serverId,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        played = played,
        favorite = favorite,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = playbackPositionTicks,
        premiereDate = premiereDate,
//        genres = genres,
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
        endDate = endDate,
        unplayedItemCount = unplayedItemCount,
    )
}
