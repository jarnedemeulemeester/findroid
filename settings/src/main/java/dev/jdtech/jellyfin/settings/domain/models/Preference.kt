package dev.jdtech.jellyfin.settings.domain.models

data class Preference<T>(
    val backendName: String,
    val defaultValue: T,
)
