package dev.jdtech.jellyfin.player.core.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class PlayerItem(
    val name: String,
    val itemId: UUID,
    val mediaType: PlayerMediaType = PlayerMediaType.UNKNOWN,
    val mediaSourceId: String,
    val playbackPosition: Long,
    val mediaSourceUri: String = "",
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val indexNumberEnd: Int? = null,
    val seriesName: String? = null,
    val externalSubtitles: List<ExternalSubtitle> = emptyList(),
    val chapters: List<PlayerChapter> = emptyList(),
    val trickplayInfo: TrickplayInfo? = null,
    val images: PlayerImages
) : Parcelable

enum class PlayerMediaType {
    MOVIE,
    EPISODE,
    UNKNOWN
}
