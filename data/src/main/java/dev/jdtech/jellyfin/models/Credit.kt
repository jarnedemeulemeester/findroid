package dev.jdtech.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Credit(
    @SerialName("Credits")
    val credit: Credits,
)

@Serializable
data class Credits(
    @SerialName("IntroStart")
    val introStart: Double,
    @SerialName("IntroEnd")
    val introEnd: Double,
    @SerialName("ShowSkipPromptAt")
    val showSkipPromptAt: Double,
    @SerialName("HideSkipPromptAt")
    val hideSkipPromptAt: Double,
)

fun CreditDto.toCredit(): Credit {
    return Credit(
        credit = Credits(
            introStart = start,
            introEnd = end,
            showSkipPromptAt = showAt,
            hideSkipPromptAt = hideAt,
        ),
    )
}
