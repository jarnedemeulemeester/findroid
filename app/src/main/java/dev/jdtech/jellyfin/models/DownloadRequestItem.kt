package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class DownloadRequestItem(
    val uri: String,
    val itemId: UUID,
    val item: DownloadItem
) : Parcelable