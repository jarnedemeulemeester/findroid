package dev.jdtech.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Intro(
    @SerialName("IntroStart")
    val introStart: Double,
    @SerialName("IntroEnd")
    val introEnd: Double,
    @SerialName("ShowSkipPromptAt")
    val showSkipPromptAt: Double,
    @SerialName("HideSkipPromptAt")
    val hideSkipPromptAt: Double,
)

fun IntroDto.toIntro(): Intro {
    return Intro(
        introStart = start,
        introEnd = end,
        showSkipPromptAt = showAt,
        hideSkipPromptAt = hideAt,
    )
}
