package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class DownloadSeriesMetadata(
    val itemId: UUID,
    val name: String? = null,
    val episodes: List<PlayerItem>
) : Parcelable