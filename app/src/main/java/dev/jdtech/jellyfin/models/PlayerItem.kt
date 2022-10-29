package dev.jdtech.jellyfin.models

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerItem(
    val name: String?,
    val itemId: UUID,
    val mediaSourceId: String,
    val playbackPosition: Long,
    val mediaSourceUri: String = "",
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val item: DownloadItem? = null,
    val externalSubtitles: List<ExternalSubtitle> = emptyList()
) : Parcelable
