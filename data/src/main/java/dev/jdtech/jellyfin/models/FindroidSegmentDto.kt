package dev.jdtech.jellyfin.models

import androidx.room.Entity
import java.util.UUID

@Entity(tableName = "segments", primaryKeys = ["itemId", "type"])
data class FindroidSegmentDto(
    val itemId: UUID,
    val type: FindroidSegmentType,
    val startTicks: Long,
    val endTicks: Long,
)

fun FindroidSegment.toFindroidSegmentsDto(itemId: UUID): FindroidSegmentDto {
    return FindroidSegmentDto(
        itemId = itemId,
        type = type,
        startTicks = startTicks,
        endTicks = endTicks,
    )
}
