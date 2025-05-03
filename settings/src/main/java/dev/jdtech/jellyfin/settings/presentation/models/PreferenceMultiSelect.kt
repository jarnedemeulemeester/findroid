package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

data class PreferenceMultiSelect(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.PHONE, DeviceType.TV),
    val onUpdate: (Set<String>?) -> Unit = {},
    val backendPreference: PreferenceBackend<Set<String>?>, // Backend preference stores a Set<String>
    val options: Int, // Resource ID for the array of entry strings
    val optionValues: Int, // Resource ID for the array of entry values
    val value: Set<String>? = null, // The current value is a Set of strings
) : Preference
