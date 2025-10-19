package dev.jdtech.jellyfin.film.presentation.media

import dev.jdtech.jellyfin.models.JellyCastCollection

sealed interface MediaAction {
    data class OnItemClick(val item: JellyCastCollection) : MediaAction
    data object OnFavoritesClick : MediaAction
    data object OnRetryClick : MediaAction
}
