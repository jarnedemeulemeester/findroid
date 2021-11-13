package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class DownloadMetadata(
    val id: UUID,
    val type: String?,
    val seriesName: String? = null,
    val name: String? = null,
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null,
    val playbackPosition: Long? = null,
    val playedPercentage: Double? = null,
    val seriesId: UUID? = null,
    val played: Boolean? = null,
    val overview: String? = null
) : Parcelable