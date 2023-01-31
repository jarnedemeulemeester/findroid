@file:Suppress("Unused")

package dev.jdtech.jellyfin.models

data class VideoMetadata(
    val resolution: List<Resolution>,
    val displayProfile: List<DisplayProfile>,
    val audio: List<AudioChannel>
)

enum class Resolution(val raw: String) {
    SD("SD"),
    HD("1080p"),
    UHD("4K");
}

enum class DisplayProfile(val raw: String) {
    SDR("SDR"),
    HDR("HDR"),
    HDR10("HDR10"),
    DOLBY_VISION("DOLBY VISION"),
    HLG("HLG");
}

enum class AudioChannel(val raw: String) {
    CH_2_0("2.0"),
    CH_2_1("2.1"),
    CH_5_1("5.1"),
    CH_7_1("7.1");
}
