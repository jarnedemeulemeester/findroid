package dev.jdtech.jellyfin.film.presentation.library

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy

sealed interface LibraryAction {
    data class OnItemClick(val item: FindroidItem) : LibraryAction
    data object OnBackClick : LibraryAction
    data class ChangeSorting(val sortBy: SortBy) : LibraryAction
}
