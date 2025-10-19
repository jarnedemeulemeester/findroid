package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "seasons",
    foreignKeys = [
        ForeignKey(
            entity = JellyCastShowDto::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("seriesId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("seriesId"),
    ],
)
data class JellyCastSeasonDto(
    @PrimaryKey
    val id: UUID,
    val seriesId: UUID,
    val name: String,
    val seriesName: String,
    val overview: String,
    val indexNumber: Int,
)

fun JellyCastSeason.toJellyCastSeasonDto(): JellyCastSeasonDto {
    return JellyCastSeasonDto(
        id = id,
        seriesId = seriesId,
        name = name,
        seriesName = seriesName,
        overview = overview,
        indexNumber = indexNumber,
    )
}
