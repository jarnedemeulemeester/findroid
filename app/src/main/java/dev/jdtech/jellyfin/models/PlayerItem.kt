package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class PlayerItem(
    val name: String?,
    val itemId: UUID,
    val mediaSourceId: String,
    val playbackPosition: Long
) : Parcelable