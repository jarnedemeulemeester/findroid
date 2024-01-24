package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerChapter(
    /**
     * The start position ticks.
     */
    val startPositionTicks: Long,
    /**
     * The name.
     */
    val name: String? = null,
) : Parcelable
