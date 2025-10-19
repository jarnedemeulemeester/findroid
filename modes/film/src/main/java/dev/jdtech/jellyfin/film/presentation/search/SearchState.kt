package dev.jdtech.jellyfin.film.presentation.search

import dev.jdtech.jellyfin.models.JellyCastItem

data class SearchState(
    val items: List<JellyCastItem> = emptyList(),
    val loading: Boolean = false,
)
