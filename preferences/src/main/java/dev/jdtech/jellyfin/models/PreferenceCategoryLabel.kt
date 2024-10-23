package dev.jdtech.jellyfin.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceCategoryLabel(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = false,
    override val dependencies: List<String> = emptyList(),
) : Preference
