package dev.jdtech.jellyfin.settings.presentation.settings

import dev.jdtech.jellyfin.models.Preference

sealed interface SettingsAction {
    data object OnBackClick : SettingsAction
    data class OnUpdate(val preference: Preference) : SettingsAction
}
