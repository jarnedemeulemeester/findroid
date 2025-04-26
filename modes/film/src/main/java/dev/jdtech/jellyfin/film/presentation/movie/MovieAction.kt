package dev.jdtech.jellyfin.film.presentation.movie

sealed interface MovieAction {
    data class Play(val startFromBeginning: Boolean = false) : MovieAction
    data class PlayTrailer(val trailer: String) : MovieAction
    data object MarkAsPlayed : MovieAction
    data object UnmarkAsPlayed : MovieAction
    data object MarkAsFavorite : MovieAction
    data object UnmarkAsFavorite : MovieAction
    data object OnBackClick : MovieAction
}
