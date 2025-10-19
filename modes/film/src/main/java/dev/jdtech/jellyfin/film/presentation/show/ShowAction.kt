package dev.jdtech.jellyfin.film.presentation.show

import dev.jdtech.jellyfin.models.JellyCastItem
import java.util.UUID

sealed interface ShowAction {
    data class Play(val startFromBeginning: Boolean = false) : ShowAction
    data class PlayTrailer(val trailer: String) : ShowAction
    data object MarkAsPlayed : ShowAction
    data object UnmarkAsPlayed : ShowAction
    data object MarkAsFavorite : ShowAction
    data object UnmarkAsFavorite : ShowAction
    data object OnBackClick : ShowAction
    data class NavigateToItem(val item: JellyCastItem) : ShowAction
    data class NavigateToPerson(val personId: UUID) : ShowAction
}
