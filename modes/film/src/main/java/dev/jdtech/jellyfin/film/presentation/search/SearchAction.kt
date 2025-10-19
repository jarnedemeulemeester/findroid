package dev.jdtech.jellyfin.film.presentation.search

import dev.jdtech.jellyfin.models.JellyCastItem

sealed interface SearchAction {
    data class Search(val query: String) : SearchAction
    data class OnItemClick(val item: JellyCastItem) : SearchAction
}
