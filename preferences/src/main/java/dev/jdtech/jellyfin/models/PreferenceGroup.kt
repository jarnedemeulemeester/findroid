package dev.jdtech.jellyfin.models

import androidx.annotation.StringRes

data class PreferenceGroup(
    @StringRes val nameStringResource: Int? = null,
    val preferences: List<Preference>,
)
