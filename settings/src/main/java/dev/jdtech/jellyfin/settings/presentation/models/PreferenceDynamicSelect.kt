package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType

/**
 * Like [PreferenceSelect] but options are provided as a runtime list instead of @ArrayRes
 * IDs. Use this when the option labels must be computed at runtime (e.g. storage volumes
 * with free-space annotations).
 *
 * [dynamicOptions] is a list of (storedValue, displayLabel) pairs. The stored value is what
 * gets written to [backendPreference]; null is allowed (maps to "not set").
 */
data class PreferenceDynamicSelect(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.PHONE, DeviceType.TV),
    val onUpdate: (String?) -> Unit = {},
    val backendPreference: PreferenceBackend<String?>,
    val dynamicOptions: List<Pair<String?, String>> = emptyList(),
    val value: String? = null,
) : Preference
