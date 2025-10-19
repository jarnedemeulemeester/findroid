package dev.jdtech.jellyfin.film.presentation.person

import dev.jdtech.jellyfin.models.JellyCastItem

sealed interface PersonAction {
    data object NavigateBack : PersonAction
    data class NavigateToItem(val item: JellyCastItem) : PersonAction
}
