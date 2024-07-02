package dev.jdtech.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FindroidSegments(
    @SerialName("Introduction")
    val intro: FindroidSegment?,
    @SerialName("Credits")
    val credit: FindroidSegment?,
)

@Serializable
data class FindroidSegment(
    val type: String = "none",
    val skip: Boolean = false,
    @SerialName("IntroStart")
    val startTime: Double,
    @SerialName("IntroEnd")
    val endTime: Double,
    @SerialName("ShowSkipPromptAt")
    val showAt: Double,
    @SerialName("HideSkipPromptAt")
    val hideAt: Double,
)

fun FindroidSegmentsDto.toFindroidSegments(): List<FindroidSegment> {
    return segments.map { segment ->
        FindroidSegment(
            type = segment.type,
            skip = segment.skip,
            startTime = segment.startTime,
            endTime = segment.endTime,
            showAt = segment.showAt,
            hideAt = segment.hideAt,
        )
    }
}
