package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceNumberInput(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<String> = emptyList(),
    val onClick: (Preference) -> Unit = {},
    val backendName: String,
    val prefix: String? = null,
    val suffix: String? = null,
    val value: Int = -1,
) : Preference
