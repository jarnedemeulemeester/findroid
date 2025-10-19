package dev.jdtech.jellyfin.film.presentation.collection

import dev.jdtech.jellyfin.models.JellyCastItem

sealed interface CollectionAction {
    data class OnItemClick(val item: JellyCastItem) : CollectionAction
    data object OnBackClick : CollectionAction
    data class SelectGenre(val genre: String?) : CollectionAction
}
