package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "trickplayInfos",
    foreignKeys = [
        ForeignKey(
            entity = JellyCastSourceDto::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("sourceId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class JellyCastTrickplayInfoDto(
    @PrimaryKey
    val sourceId: String,
    val width: Int,
    val height: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val thumbnailCount: Int,
    val interval: Int,
    val bandwidth: Int,
)

fun JellyCastTrickplayInfo.toJellyCastTrickplayInfoDto(sourceId: String): JellyCastTrickplayInfoDto {
    return JellyCastTrickplayInfoDto(
        sourceId = sourceId,
        width = width,
        height = height,
        tileWidth = tileWidth,
        tileHeight = tileHeight,
        thumbnailCount = thumbnailCount,
        interval = interval,
        bandwidth = bandwidth,
    )
}
