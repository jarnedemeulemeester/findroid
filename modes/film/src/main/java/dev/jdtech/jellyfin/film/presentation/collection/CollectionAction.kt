package dev.jdtech.jellyfin.film.presentation.collection

import dev.jdtech.jellyfin.models.FindroidItem

sealed interface CollectionAction {
    data class OnItemClick(val item: FindroidItem) : CollectionAction
    data object OnBackClick : CollectionAction
}
