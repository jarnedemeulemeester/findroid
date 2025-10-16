package dev.jdtech.jellyfin.film.presentation.search

import dev.jdtech.jellyfin.models.FindroidItem

data class SearchState(
    val items: List<FindroidItem> = emptyList(),
    val loading: Boolean = false,
)
