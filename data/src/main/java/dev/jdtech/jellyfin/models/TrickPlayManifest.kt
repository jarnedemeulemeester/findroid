package dev.jdtech.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrickPlayManifest(
    @SerialName("Version")
    val version: String,
    @SerialName("WidthResolutions")
    val widthResolutions: List<Int>,
)

fun TrickPlayManifestDto.toTrickPlayManifest(): TrickPlayManifest {
    return TrickPlayManifest(
        version = version,
        widthResolutions = listOf(resolution),
    )
}
