package dev.jdtech.jellyfin.film.presentation.episode

sealed interface EpisodeAction {
    data object OnPlayClick : EpisodeAction
}
