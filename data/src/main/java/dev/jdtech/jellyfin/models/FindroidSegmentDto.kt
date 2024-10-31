package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import org.jellyfin.sdk.model.api.MediaSegmentType
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
    val type: MediaSegmentType,
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
