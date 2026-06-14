package dev.jdtech.jellyfin.offline.download

enum class OfflineProfileKind {
    DEFAULT_480P,
    HIGH_720P,
    ORIGINAL,
}

data class OfflineProfile(
    val id: String,
    val kind: OfflineProfileKind,
    val container: String,
    val videoCodec: String?,
    val audioCodec: String?,
    val maxHeight: Int?,
    val videoBitrateBitsPerSecond: Int?,
    val audioBitrateBitsPerSecond: Int?,
    val preserveOriginal: Boolean = false,
) {
    companion object {
        val Default480p =
            OfflineProfile(
                id = "default-480p",
                kind = OfflineProfileKind.DEFAULT_480P,
                container = "mp4",
                videoCodec = "h264",
                audioCodec = "aac",
                maxHeight = 480,
                videoBitrateBitsPerSecond = 1_800_000,
                audioBitrateBitsPerSecond = 160_000,
            )

        val High720p =
            OfflineProfile(
                id = "high-720p",
                kind = OfflineProfileKind.HIGH_720P,
                container = "mp4",
                videoCodec = "h264",
                audioCodec = "aac",
                maxHeight = 720,
                videoBitrateBitsPerSecond = 3_000_000,
                audioBitrateBitsPerSecond = 192_000,
            )

        val Original =
            OfflineProfile(
                id = "original",
                kind = OfflineProfileKind.ORIGINAL,
                container = "source",
                videoCodec = null,
                audioCodec = null,
                maxHeight = null,
                videoBitrateBitsPerSecond = null,
                audioBitrateBitsPerSecond = null,
                preserveOriginal = true,
            )
    }
}
