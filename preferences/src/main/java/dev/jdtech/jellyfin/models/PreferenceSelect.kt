package dev.jdtech.jellyfin.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceSelect(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val disabled: Boolean = false,
    override val onClick: (Preference) -> Unit = {},
    val backendName: String,
    val backendDefaultValue: String,
) : Preference
