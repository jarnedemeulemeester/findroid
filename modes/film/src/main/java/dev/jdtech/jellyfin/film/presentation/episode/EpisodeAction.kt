package dev.jdtech.jellyfin.film.presentation.episode

sealed interface EpisodeAction {
    data object OnPlayClick : EpisodeAction
    data object MarkAsPlayed : EpisodeAction
    data object UnmarkAsPlayed : EpisodeAction
    data object MarkAsFavorite : EpisodeAction
    data object UnmarkAsFavorite : EpisodeAction
}
