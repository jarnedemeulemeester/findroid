package dev.jdtech.jellyfin.settings.presentation.settings

import android.content.Intent

sealed interface SettingsEvent {
    data object NavigateToUsers : SettingsEvent

    data object NavigateToServers : SettingsEvent

    data object NavigateToAbout : SettingsEvent

    data class NavigateToSettings(val indexes: IntArray) : SettingsEvent

    data class UpdateTheme(val theme: String) : SettingsEvent

    data class LaunchIntent(val intent: Intent) : SettingsEvent

    data object RestartActivity : SettingsEvent
}
