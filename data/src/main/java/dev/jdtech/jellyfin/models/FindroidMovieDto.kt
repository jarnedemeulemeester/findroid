package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "movies")
data class FindroidMovieDto(
    @PrimaryKey
    val id: UUID,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val playedPercentage: Float?,
    val played: Boolean,
    val favorite: Boolean,
    val runtimeTicks: Long,
    val playbackPositionTicks: Long,
//    val premiereDate: DateTime?,
//    val genres: List<String>,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
//    val endDate: DateTime?,
    val unplayedItemCount: Int? = null,
)

fun JellyfinMovieItem.toFindroidMovieDto(): FindroidMovieDto {
    return FindroidMovieDto(
        id = id,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        playedPercentage = playedPercentage,
        played = played,
        favorite = favorite,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = playbackPositionTicks,
//        premiereDate = premiereDate,
//        genres = genres,
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
//        endDate = endDate,
        unplayedItemCount = unplayedItemCount,
    )
}
