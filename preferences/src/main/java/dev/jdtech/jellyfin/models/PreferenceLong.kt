package dev.jdtech.jellyfin.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceLong(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<String> = emptyList(),
    val onClick: (Preference) -> Unit = {},
    val backendName: String,
    val backendDefaultValue: Long,
    val value: Long = 0L,
) : Preference
