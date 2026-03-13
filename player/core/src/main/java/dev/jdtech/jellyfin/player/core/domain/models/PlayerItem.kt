package dev.jdtech.jellyfin.player.core.domain.models

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerItem(
    val name: String,
    val itemId: UUID,
    val mediaSourceId: String,
    val playbackPosition: Long,
    val mediaSourceUri: String = "",
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val indexNumberEnd: Int? = null,
    val externalSubtitles: List<ExternalSubtitle> = emptyList(),
    val chapters: List<PlayerChapter> = emptyList(),
    val trickplayInfo: TrickplayInfo? = null,
    val people: List<PlayerPerson> = emptyList(),
    val overview: String = "",
    /** Episode/movie backdrop URI string for next-up cards (null for non-visual items). */
    val backdropImageUri: String? = null,
    /** Series name for episodes, used in next-up panel display. */
    val seriesName: String? = null,
) : Parcelable
