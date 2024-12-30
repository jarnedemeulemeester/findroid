package dev.jdtech.jellyfin.settings.presentation.settings

import dev.jdtech.jellyfin.models.Preference

data class SettingsState(
    val isLoading: Boolean = false,
    val preferences: List<Preference> = emptyList(),
)
