package dev.jdtech.jellyfin.settings.presentation.models

import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

data class PreferenceButton(
    override val nameStringResource: Int,
    override val descriptionStringRes: Int? = null,
    val descriptionString: String? = null,  // Dynamic description
    override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val supportedDeviceTypes: List<DeviceType> = DeviceType.entries,
    val onClick: () -> Unit = {},
) : Preference
