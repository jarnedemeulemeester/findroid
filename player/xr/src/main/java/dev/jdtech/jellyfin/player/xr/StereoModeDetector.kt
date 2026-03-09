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

    // Pre-compiled patterns for efficient matching, ordered by specificity.
    // Dot-separated tags like ".hsbs." and ".3d." are matched because \b treats "." as a boundary.
    private val SBS_PATTERNS = listOf(
        // Half SBS patterns (most common for SBS content)
        Regex("""\bhsbs\b"""),
        Regex("""\bh[.\s-]?sbs\b"""),
        Regex("""\bhalf[\s.-]?sbs\b"""),
        // Full SBS
        Regex("""\bfsbs\b"""),
        Regex("""\bf[.\s-]?sbs\b"""),
        Regex("""\bfull[\s.-]?sbs\b"""),
        // Generic SBS
        Regex("""\bsbs\b"""),
        Regex("""\bside[\s.-]by[\s.-]side\b"""),
        // Combined 3D+SBS tag (e.g., "3d.hsbs", "3d.sbs")
        Regex("""\b3d[\s.-]?h?sbs\b"""),
    )

    private val TB_PATTERNS = listOf(
        // Half Over-Under / Top-Bottom
        Regex("""\bhou\b"""),
        Regex("""\bhalf[\s.-]?ou\b"""),
        Regex("""\bhtab\b"""),
        Regex("""\bhtb\b"""),
        Regex("""\bh[.\s-]?tab\b"""),
        Regex("""\bhalf[\s.-]?tab\b"""),
        // Full Over-Under
        Regex("""\bfou\b"""),
        Regex("""\bfull[\s.-]?ou\b"""),
        Regex("""\bftab\b"""),
        Regex("""\bf[.\s-]?tab\b"""),
        Regex("""\bfull[\s.-]?tab\b"""),
        // Generic Over-Under / Top-Bottom
        Regex("""\b[ot]ab\b"""),
        Regex("""\bover[\s.-]under\b"""),
        Regex("""\btop[\s.-]and[\s.-]bottom\b"""),
        // Combined 3D+TB tag
        Regex("""\b3d[\s.-]?h?tab\b"""),
        Regex("""\b3d[\s.-]?h?ou\b"""),
    )

    private val GENERIC_3D_PATTERN = Regex("""\b3d\b""")

    fun detectFromName(name: String): StereoMode {
        val lower = name.lowercase()
        // Check specific SBS patterns first
        if (SBS_PATTERNS.any { it.containsMatchIn(lower) }) return StereoMode.SIDE_BY_SIDE
        // Check specific TB patterns
        if (TB_PATTERNS.any { it.containsMatchIn(lower) }) return StereoMode.TOP_BOTTOM
        // Generic 3D tag - default to SBS as it's most common
        if (GENERIC_3D_PATTERN.containsMatchIn(lower)) return StereoMode.SIDE_BY_SIDE
        return StereoMode.MONO
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
     * Also checks source names (filenames) which often contain 3D tags like ".hsbs.", ".3d.sbs." etc.
     */
    fun detect(movieName: String, video3DFormat: String? = null, sourceNames: List<String> = emptyList()): StereoMode {
        val fromApi = detectFromVideo3DFormat(video3DFormat)
        if (fromApi != StereoMode.MONO) return fromApi

        // Check source names (actual filenames) first, as they more reliably contain 3D tags
        for (sourceName in sourceNames) {
            val fromSource = detectFromName(sourceName)
            if (fromSource != StereoMode.MONO) return fromSource
        }

        return detectFromName(movieName)
    }

    /**
     * Check if the device supports Android XR immersive features.
     */
    fun isXrDevice(context: android.content.Context): Boolean {
        return try {
            context.packageManager.hasSystemFeature("android.software.xr.api.spatial") ||
            context.packageManager.hasSystemFeature("android.software.xr.immersive") ||
            context.packageManager.hasSystemFeature("android.hardware.xr.immersive") ||
            android.os.Build.MODEL.contains("XR", ignoreCase = true) ||
            android.os.Build.MANUFACTURER.contains("Samsung", ignoreCase = true) && android.os.Build.DEVICE.contains("xr", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
