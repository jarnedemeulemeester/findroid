package dev.jdtech.jellyfin.film.presentation.episode

import java.util.UUID

sealed interface EpisodeAction {
    data class Play(val startFromBeginning: Boolean = false) : EpisodeAction
    data class PlayTrailer(val trailer: String) : EpisodeAction
    data object MarkAsPlayed : EpisodeAction
    data object UnmarkAsPlayed : EpisodeAction
    data object MarkAsFavorite : EpisodeAction
    data object UnmarkAsFavorite : EpisodeAction
    data object Download : EpisodeAction
    data object OnBackClick : EpisodeAction
    data class NavigateToPerson(val personId: UUID) : EpisodeAction
}
