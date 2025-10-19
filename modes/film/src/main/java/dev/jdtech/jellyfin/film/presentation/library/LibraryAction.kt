package dev.jdtech.jellyfin.film.presentation.library

import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.SortBy
import org.jellyfin.sdk.model.api.SortOrder

sealed interface LibraryAction {
    data class OnItemClick(val item: JellyCastItem) : LibraryAction
    data object OnBackClick : LibraryAction
    data class ChangeSorting(val sortBy: SortBy, val sortOrder: SortOrder) : LibraryAction
    data class SelectGenre(val genre: String?) : LibraryAction
}
