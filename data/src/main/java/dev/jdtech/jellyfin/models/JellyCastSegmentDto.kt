package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "segments",
    primaryKeys = ["itemId", "type"],
    foreignKeys = [
        ForeignKey(
            entity = JellyCastEpisodeDto::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("itemId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class JellyCastSegmentDto(
    val itemId: UUID,
    val type: JellyCastSegmentType,
    val startTicks: Long,
    val endTicks: Long,
)

fun JellyCastSegment.toJellyCastSegmentsDto(itemId: UUID): JellyCastSegmentDto {
    return JellyCastSegmentDto(
        itemId = itemId,
        type = type,
        startTicks = startTicks,
        endTicks = endTicks,
    )
}
