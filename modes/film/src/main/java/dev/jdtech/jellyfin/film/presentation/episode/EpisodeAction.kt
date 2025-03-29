package dev.jdtech.jellyfin.film.presentation.episode

sealed interface EpisodeAction {
    data class Play(val startFromBeginning: Boolean = false) : EpisodeAction
    data object MarkAsPlayed : EpisodeAction
    data object UnmarkAsPlayed : EpisodeAction
    data object MarkAsFavorite : EpisodeAction
    data object UnmarkAsFavorite : EpisodeAction
    data object OnBackClick : EpisodeAction
}
