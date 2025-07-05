package dev.jdtech.jellyfin.film.presentation.media

import dev.jdtech.jellyfin.models.FindroidCollection

sealed interface MediaAction {
    data class OnItemClick(val item: FindroidCollection) : MediaAction
    data object OnFavoritesClick : MediaAction
    data object OnRetryClick : MediaAction
    data object OnSettingsClick : MediaAction
}
