package dev.jdtech.jellyfin.settings.presentation.settings

sealed interface SettingsAction {
    data object OnBackClick : SettingsAction
}
