package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "seasons")
data class FindroidSeasonDto(
    @PrimaryKey
    val id: UUID,
    val seriesId: UUID,
    val name: String,
    val seriesName: String,
    val overview: String,
    val indexNumber: Int,
    val played: Boolean,
    val favorite: Boolean,
    val unplayedItemCount: Int? = null,
)

fun FindroidSeason.toFindroidSeasonDto(): FindroidSeasonDto {
    return FindroidSeasonDto(
        id = id,
        seriesId = seriesId,
        name = name,
        seriesName = seriesName,
        overview = overview,
        indexNumber = indexNumber,
        played = played,
        favorite = favorite,
        unplayedItemCount = unplayedItemCount,
    )
}
