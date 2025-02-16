package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

data class PreferenceCategory(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.PHONE, DeviceType.TV),
    val onClick: (Preference) -> Unit = {},
    val nestedPreferenceGroups: List<PreferenceGroup> = emptyList(),
) : Preference
