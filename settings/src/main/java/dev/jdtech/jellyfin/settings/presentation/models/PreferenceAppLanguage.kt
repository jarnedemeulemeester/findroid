package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType

data class PreferenceAppLanguage(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.PHONE, DeviceType.TV),
) : Preference
