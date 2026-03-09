package dev.jdtech.jellyfin.player.xr

/**
 * Detects the stereoscopic 3D format of video content from metadata.
 */
object StereoModeDetector {

    enum class StereoMode {
        MONO,
        SIDE_BY_SIDE,
        TOP_BOTTOM,
    }

    /**
     * Detect stereo mode from the video file name or movie title.
     * Checks for common naming conventions used in 3D video files.
     */
    fun detectFromName(name: String): StereoMode {
        val lower = name.lowercase()
        return when {
            // Half SBS patterns (most common for SBS content)
            Regex("""\bhsbs\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            Regex("""\bhalf[\s-]?sbs\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            // Full SBS
            Regex("""\bfsbs\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            Regex("""\bfull[\s-]?sbs\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            // Generic SBS
            Regex("""\bsbs\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            Regex("""\bside[\s-]by[\s-]side\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            // Half Over-Under (Top-Bottom)
            Regex("""\bhou\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            Regex("""\bhalf[\s-]?ou\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            // Full Over-Under
            Regex("""\bfou\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            Regex("""\bfull[\s-]?ou\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            // Generic Over-Under / Top-Bottom
            Regex("""\b[ot]ab\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            Regex("""\bover[\s-]under\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            Regex("""\btop[\s-]and[\s-]bottom\b""").containsMatchIn(lower) -> StereoMode.TOP_BOTTOM
            // Generic 3D tag - default to SBS as it's most common
            Regex("""\b3d\b""").containsMatchIn(lower) -> StereoMode.SIDE_BY_SIDE
            else -> StereoMode.MONO
        }
    }

    /**
     * Detect stereo mode from Jellyfin's Video3DFormat field.
     */
    fun detectFromVideo3DFormat(format: String?): StereoMode {
        if (format == null) return StereoMode.MONO
        return when (format.uppercase()) {
            "HALF_SIDE_BY_SIDE", "FULL_SIDE_BY_SIDE" -> StereoMode.SIDE_BY_SIDE
            "HALF_TOP_AND_BOTTOM", "FULL_TOP_AND_BOTTOM" -> StereoMode.TOP_BOTTOM
            "MVC" -> StereoMode.SIDE_BY_SIDE
            else -> StereoMode.MONO
        }
    }

    /**
     * Detect stereo mode using all available metadata, preferring API data over filename.
     */
    fun detect(movieName: String, video3DFormat: String? = null): StereoMode {
        val fromApi = detectFromVideo3DFormat(video3DFormat)
        if (fromApi != StereoMode.MONO) return fromApi
        return detectFromName(movieName)
    }

    /**
     * Check if the device supports Android XR immersive features.
     */
    fun isXrDevice(context: android.content.Context): Boolean {
        return try {
            context.packageManager.hasSystemFeature("android.hardware.xr.immersive")
        } catch (_: Exception) {
            false
        }
    }
}
