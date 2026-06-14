package dev.jdtech.jellyfin.offline.download

enum class OfflineItemKind {
    MOVIE,
    EPISODE,
}

data class OfflineItemSnapshot(
    val packageId: String,
    val serverId: String,
    val itemId: String,
    val itemKind: OfflineItemKind,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val runtimeTicks: Long,
    val playbackPositionTicks: Long,
    val played: Boolean,
    val favorite: Boolean,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val indexNumber: Int? = null,
    val indexNumberEnd: Int? = null,
    val parentIndexNumber: Int? = null,
    val communityRating: Float? = null,
    val productionYear: Int? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
