package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType

enum class JellyCastSegmentType {
    INTRO, OUTRO, RECAP, PREVIEW, COMMERCIAL, UNKNOWN
}

private fun MediaSegmentType.toJellyCastSegmentType(): JellyCastSegmentType = when (this) {
    MediaSegmentType.UNKNOWN -> JellyCastSegmentType.UNKNOWN
    MediaSegmentType.INTRO -> JellyCastSegmentType.INTRO
    MediaSegmentType.OUTRO -> JellyCastSegmentType.OUTRO
    MediaSegmentType.RECAP -> JellyCastSegmentType.RECAP
    MediaSegmentType.PREVIEW -> JellyCastSegmentType.PREVIEW
    MediaSegmentType.COMMERCIAL -> JellyCastSegmentType.COMMERCIAL
}

data class JellyCastSegment(
    val type: JellyCastSegmentType,
    val startTicks: Long,
    val endTicks: Long,
)

fun JellyCastSegmentDto.toJellyCastSegment(): JellyCastSegment {
    return JellyCastSegment(
        type = type,
        startTicks = startTicks,
        endTicks = endTicks,
    )
}

fun MediaSegmentDto.toJellyCastSegment(): JellyCastSegment {
    return JellyCastSegment(
        type = type.toJellyCastSegmentType(),
        startTicks = startTicks / 10000,
        endTicks = endTicks / 10000,
    )
}
