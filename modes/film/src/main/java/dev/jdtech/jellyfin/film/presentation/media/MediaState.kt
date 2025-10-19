package dev.jdtech.jellyfin.film.presentation.media

import dev.jdtech.jellyfin.models.JellyCastCollection

data class MediaState(
    val libraries: List<JellyCastCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
