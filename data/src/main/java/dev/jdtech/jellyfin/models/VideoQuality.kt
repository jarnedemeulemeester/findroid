package dev.jdtech.jellyfin.models

enum class VideoQuality(
    val bitrate: Int,
    val height: Int,
    val width: Int,
    val isOriginalQuality: Boolean,
) {
    Auto(10000000, 1080, 1920, false),
    Original(1000000000, 1080, 1920, true),
    P3840(12000000,3840, 2160, false), // Here for future proofing and to calculate original resolution only
    P1080(8000000, 1080, 1920, false),
    P720(3000000, 720, 1280, false),
    P480(1500000, 480, 854, false),
    P360(800000, 360, 640, false);

    override fun toString(): String = when (this) {
        Auto -> "Auto"
        Original -> "Original"
        P3840 -> "4K"
        else -> "${height}p"
    }

    companion object {
        fun fromString(quality: String): VideoQuality? = entries.find { it.toString() == quality }
        fun getBitrate(quality: VideoQuality): Int = quality.bitrate
        fun getHeight(quality: VideoQuality): Int = quality.height
        fun getWidth(quality: VideoQuality): Int = quality.width
        fun getIsOriginalQuality(quality: VideoQuality): Boolean = quality.isOriginalQuality
    }
}