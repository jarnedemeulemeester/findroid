@file:Suppress("Unused")

package dev.jdtech.jellyfin.models

data class VideoMetadata(
    val resolution: List<Resolution>,
    val displayProfiles: List<DisplayProfile>,
    val audioChannels: List<AudioChannel>,
    val audioCodecs: List<AudioCodec>,
    val isAtmos: List<Boolean>,
)

enum class Resolution(val raw: String) {
    SD("SD"),
    HD("HD"),
    UHD("4K"),
}

enum class DisplayProfile(val raw: String) {
    SDR("SDR"),
    HDR("HDR"),
    HDR10("HDR10"),
    DOLBY_VISION("Vision"),
    HLG("HLG"),
}

enum class AudioChannel(val raw: String) {
    CH_2_0("2.0"),
    CH_2_1("2.1"),
    CH_5_1("5.1"),
    CH_7_1("7.1"),
}

enum class AudioCodec(val raw: String) {
    FLAC("FLAC"),
    MP3("MP3"),
    AAC("AAC"),
    AC3("Digital"),
    EAC3("Digital+"),
    VORBIS("VORBIS"),
    DTS("DTS"),
    TRUEHD("TrueHD"),
    OPUS("OPUS"),
    ;

    override fun toString() = super.toString().lowercase()
}
