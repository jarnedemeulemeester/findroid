package dev.jdtech.jellyfin.settings.presentation.models

import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

interface Preference {
    val nameStringResource: Int
    val descriptionStringRes: Int?
    val iconDrawableId: Int?
    val enabled: Boolean
    val dependencies: List<PreferenceBackend<Boolean>>
    val supportedDeviceTypes: List<DeviceType>
}
