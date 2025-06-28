package dev.jdtech.jellyfin.film.presentation.season

import dev.jdtech.jellyfin.models.FindroidItem

sealed interface SeasonAction {
    data class Play(val startFromBeginning: Boolean = false) : SeasonAction
    data object MarkAsPlayed : SeasonAction
    data object UnmarkAsPlayed : SeasonAction
    data object MarkAsFavorite : SeasonAction
    data object UnmarkAsFavorite : SeasonAction
    data object OnBackClick : SeasonAction
    data class NavigateToItem(val item: FindroidItem) : SeasonAction
}
