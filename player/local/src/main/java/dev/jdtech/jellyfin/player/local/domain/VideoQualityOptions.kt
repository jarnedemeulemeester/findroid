package dev.jdtech.jellyfin.player.local.domain

/** Max streaming bitrates offered in the quality menu, same as the Jellyfin web player. */
object VideoQualityOptions {
    private val bitrates =
        listOf(
            120_000_000,
            80_000_000,
            60_000_000,
            40_000_000,
            20_000_000,
            15_000_000,
            10_000_000,
            8_000_000,
            6_000_000,
            4_000_000,
            3_000_000,
            1_500_000,
            720_000,
            420_000,
        )

    /** Bitrates up to the source bitrate, plus one above it to keep the original quality. */
    fun get(videoBitrate: Int?, videoCodec: String?): List<Int> {
        if (videoBitrate == null || videoBitrate <= 0 || videoBitrate >= bitrates.first()) {
            return bitrates
        }

        // Transcoding to H.264 needs more bitrate than these codecs
        val referenceBitrate =
            if (videoCodec in listOf("hevc", "av1", "vp9") && videoBitrate <= 20_000_000) {
                videoBitrate * 1.5
            } else {
                videoBitrate.toDouble()
            }

        return listOfNotNull(bitrates.lastOrNull { it > referenceBitrate }) +
            bitrates.filter { it <= referenceBitrate }
    }
}
