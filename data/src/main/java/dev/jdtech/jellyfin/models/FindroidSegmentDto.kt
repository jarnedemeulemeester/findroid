package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "segments",
    primaryKeys = ["itemId", "type"],
    foreignKeys = [
        ForeignKey(
            entity = FindroidEpisodeDto::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("itemId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FindroidSegmentDto(
    val itemId: UUID,
    val type: FindroidSegmentType,
    val startTime: Double,
    val endTime: Double,
    val showAt: Double,
    val hideAt: Double,
)

fun FindroidSegment.toFindroidSegmentsDto(itemId: UUID): FindroidSegmentDto {
    return FindroidSegmentDto(
        itemId = itemId,
        type = type,
        startTime = startTime,
        endTime = endTime,
        showAt = showAt,
        hideAt = hideAt,
    )
}
