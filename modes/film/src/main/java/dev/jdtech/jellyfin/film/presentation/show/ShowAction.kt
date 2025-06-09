package dev.jdtech.jellyfin.film.presentation.show

import dev.jdtech.jellyfin.models.FindroidItem

sealed interface ShowAction {
    data class Play(val startFromBeginning: Boolean = false) : ShowAction
    data class PlayTrailer(val trailer: String) : ShowAction
    data object MarkAsPlayed : ShowAction
    data object UnmarkAsPlayed : ShowAction
    data object MarkAsFavorite : ShowAction
    data object UnmarkAsFavorite : ShowAction
    data object OnBackClick : ShowAction
    data class NavigateToItem(val item: FindroidItem) : ShowAction
}
