package dev.jdtech.jellyfin.settings.presentation.settings

sealed interface SettingsFileEditAction {
    data object OnBackClick : SettingsFileEditAction
    data object OnSave : SettingsFileEditAction
}
