package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

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
    val chapters: List<PlayerChapter>? = null,
    val trickplayInfo: Map<String, TrickplayInfo>? = null,
) : Parcelable
