package dev.jdtech.jellyfin.player.core.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerImages (
    val primary: Uri? = null,
    val backdrop: Uri? = null,
    val logo: Uri? = null,
    val showPrimary: Uri? = null,
    val showBackdrop: Uri? = null,
    val showLogo: Uri? = null
) : Parcelable
