package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType

data class FindroidSegment(
    var type: MediaSegmentType = MediaSegmentType.UNKNOWN,
    val startTicks: Long,
    val endTicks: Long,
)

fun FindroidSegmentDto.toFindroidSegment(): FindroidSegment {
    return FindroidSegment(
        type = type,
        startTicks = startTicks,
        endTicks = endTicks,
    )
}

fun MediaSegmentDto.toFindroidSegment(): FindroidSegment {
    return FindroidSegment(
        type = type,
        startTicks = startTicks / 10000,
        endTicks = endTicks / 10000,
    )
}
