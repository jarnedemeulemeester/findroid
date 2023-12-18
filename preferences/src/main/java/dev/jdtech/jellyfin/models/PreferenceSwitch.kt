package dev.jdtech.jellyfin.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceSwitch(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val disabled: Boolean = false,
    val onClick: (PreferenceSwitch) -> Unit = {},
    val backendName: String,
    val backendDefaultValue: Boolean,
    val value: Boolean = false,
) : Preference
