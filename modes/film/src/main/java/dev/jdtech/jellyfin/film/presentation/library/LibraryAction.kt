package dev.jdtech.jellyfin.film.presentation.library

import dev.jdtech.jellyfin.models.FindroidItem

sealed interface LibraryAction {
    data class OnItemClick(val item: FindroidItem) : LibraryAction
}
