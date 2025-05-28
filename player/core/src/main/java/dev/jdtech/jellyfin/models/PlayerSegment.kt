package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerSegment(
    val type: FindroidSegmentType,
    val startTicks: Long,
    val endTicks: Long,
) : Parcelable
