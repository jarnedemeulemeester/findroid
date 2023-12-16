package dev.jdtech.jellyfin.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceCategory(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val disabled: Boolean = false,
    override val onClick: (Preference) -> Unit = {},
    val nestedPreferences: List<Preference> = emptyList(),
) : Preference
