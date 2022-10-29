package dev.jdtech.jellyfin.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ExternalSubtitle(
    val title: String,
    val language: String,
    val uri: Uri,
    val mimeType: String,
) : Parcelable
