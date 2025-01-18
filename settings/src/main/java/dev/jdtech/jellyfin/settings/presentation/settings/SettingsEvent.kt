package dev.jdtech.jellyfin.settings.presentation.settings

sealed interface SettingsEvent {
    data object NavigateToUsers : SettingsEvent
    data object NavigateToServers : SettingsEvent
    data class NavigateToSettings(val indexes: IntArray) : SettingsEvent
}
