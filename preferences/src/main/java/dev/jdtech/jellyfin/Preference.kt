package dev.jdtech.jellyfin

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class Preference(
    @StringRes val nameStringResource: Int,
    @DrawableRes val iconDrawableId: Int?,
    val type: PreferenceType,
    val disabled: Boolean = false,
    val onClick: () -> Unit = {},
)
