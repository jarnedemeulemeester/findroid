package dev.jdtech.jellyfin.player.core.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExternalSubtitle(
    val title: String,
    val language: String,
    val uri: Uri,
    val mimeType: String,
) : Parcelable
