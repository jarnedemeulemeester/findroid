package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "segments")
data class FindroidSegmentsDto(
    @PrimaryKey
    val itemId: UUID,
    val segments: List<FindroidSegment>,
)

fun List<FindroidSegment>.toFindroidSegmentsDto(itemId: UUID): FindroidSegmentsDto {
    return FindroidSegmentsDto(
        itemId = itemId,
        segments = this,
    )
}
