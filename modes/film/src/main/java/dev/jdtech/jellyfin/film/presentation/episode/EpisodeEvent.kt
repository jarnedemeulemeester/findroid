package dev.jdtech.jellyfin.film.presentation.episode

sealed interface EpisodeEvent {
    data object ItemDeleted : EpisodeEvent

    data object DeleteFailed : EpisodeEvent
}
