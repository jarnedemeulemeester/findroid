package dev.jdtech.jellyfin.settings.presentation.settings

import dev.jdtech.jellyfin.settings.presentation.models.Preference

sealed interface SettingsAction {
    data object OnBackClick : SettingsAction
    data class OnUpdate(val preference: Preference) : SettingsAction
}
