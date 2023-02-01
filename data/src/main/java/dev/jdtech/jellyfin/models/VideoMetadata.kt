@file:Suppress("Unused")

package dev.jdtech.jellyfin.models

data class VideoMetadata(
    val resolution: List<Resolution>,
    val displayProfiles: List<DisplayProfile>,
    val audioChannels: List<AudioChannel>,
    val audioCodecs: List<AudioCodec>
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

enum class AudioCodec(val raw: String) {
    FLAC("FLAC"),
    MP3("MP3"),
    AAC("AAC"),
    AC3("AC3"),
    EAC3("EAC3"),
    VORBIS("VORBIS"),
    DOLBY("DOLBY"),
    DTS("DTS"),
    TRUEHD("TRUEHD"),
    OPUS("OPUS");

    override fun toString() = this.raw.lowercase()
}
