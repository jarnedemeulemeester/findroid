package dev.jdtech.jellyfin.settings.presentation.settings

import dev.jdtech.jellyfin.models.PreferenceGroup

data class SettingsState(
    val isLoading: Boolean = false,
    val preferenceGroups: List<PreferenceGroup> = emptyList(),
)
