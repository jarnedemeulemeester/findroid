package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType

enum class FindroidSegmentType {
    INTRO, OUTRO, RECAP, PREVIEW, COMMERCIAL, UNKNOWN
}

private fun MediaSegmentType.toFindroidSegmentType(): FindroidSegmentType = when (this) {
    MediaSegmentType.UNKNOWN -> FindroidSegmentType.UNKNOWN
    MediaSegmentType.INTRO -> FindroidSegmentType.INTRO
    MediaSegmentType.OUTRO -> FindroidSegmentType.OUTRO
    MediaSegmentType.RECAP -> FindroidSegmentType.RECAP
    MediaSegmentType.PREVIEW -> FindroidSegmentType.PREVIEW
    MediaSegmentType.COMMERCIAL -> FindroidSegmentType.COMMERCIAL
}

data class FindroidSegment(
    val type: FindroidSegmentType,
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
        type = type.toFindroidSegmentType(),
        startTicks = startTicks / 10000,
        endTicks = endTicks / 10000,
    )
}
