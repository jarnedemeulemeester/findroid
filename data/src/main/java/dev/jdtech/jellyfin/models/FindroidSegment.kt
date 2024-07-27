package dev.jdtech.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class FindroidSegmentType {
    INTRO, CREDITS, UNKNOWN
}

@Serializable
data class FindroidSegment(
    var type: FindroidSegmentType = FindroidSegmentType.UNKNOWN,
    @SerialName("IntroStart")
    val startTime: Double,
    @SerialName("IntroEnd")
    val endTime: Double,
    @SerialName("ShowSkipPromptAt")
    val showAt: Double,
    @SerialName("HideSkipPromptAt")
    val hideAt: Double,
)

fun FindroidSegmentDto.toFindroidSegment(): FindroidSegment {
    return FindroidSegment(
        type = type,
        startTime = startTime,
        endTime = endTime,
        showAt = showAt,
        hideAt = hideAt,
    )
}
