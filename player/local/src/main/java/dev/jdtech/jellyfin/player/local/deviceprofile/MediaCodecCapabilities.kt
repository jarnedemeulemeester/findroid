package dev.jdtech.jellyfin.player.local.deviceprofile

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.Size

/** Video decoding capabilities of the device. */
internal class MediaCodecCapabilities {
    private val decoders: List<MediaCodecInfo> by lazy {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder }
    }

    private fun decodersFor(mime: String): List<MediaCodecInfo> = decoders.filter { info ->
        info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
    }

    private fun capabilitiesFor(mime: String): List<MediaCodecInfo.CodecCapabilities> =
        decodersFor(mime).mapNotNull { info ->
            try {
                info.getCapabilitiesForType(mime)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

    fun supports(mime: String): Boolean = decodersFor(mime).isNotEmpty()

    fun supportsHardware(mime: String): Boolean = decodersFor(mime).any { it.isHardwareAccelerated }

    fun supportsProfile(mime: String, profile: Int): Boolean =
        capabilitiesFor(mime).any { capabilities ->
            capabilities.profileLevels.any { it.profile == profile }
        }

    fun maxLevel(mime: String, profile: Int): Int =
        capabilitiesFor(mime)
            .flatMap { it.profileLevels.asList() }
            .filter { it.profile == profile }
            .maxOfOrNull { it.level } ?: 0

    fun maxResolution(mime: String): Size? {
        val videoCapabilities = capabilitiesFor(mime).mapNotNull { it.videoCapabilities }
        if (videoCapabilities.isEmpty()) return null
        return Size(
            videoCapabilities.maxOf { it.supportedWidths.upper },
            videoCapabilities.maxOf { it.supportedHeights.upper },
        )
    }
}
