package dev.jdtech.jellyfin.settings.presentation.models

import dev.jdtech.jellyfin.settings.domain.models.Permission
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType

/** A [Permission] presented as a row in the settings Permissions category. */
data class PreferencePermission(
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.PHONE, DeviceType.TV),
    val permission: Permission,
) : Preference {
    override val nameStringResource: Int
        get() = permission.nameStringResource

    override val descriptionStringRes: Int?
        get() = permission.descriptionStringRes

    override val iconDrawableId: Int
        get() = permission.iconDrawableId

    override val minSdk: Int
        get() = permission.minSdk
}
