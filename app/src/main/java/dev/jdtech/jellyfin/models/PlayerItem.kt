package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class PlayerItem(
    val itemId: UUID,
    val mediaSourceId: String
) : Parcelable