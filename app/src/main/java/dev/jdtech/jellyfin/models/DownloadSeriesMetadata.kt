package dev.jdtech.jellyfin.models

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadSeriesMetadata(
    val itemId: UUID,
    val name: String? = null,
    val episodes: List<PlayerItem>
) : Parcelable
