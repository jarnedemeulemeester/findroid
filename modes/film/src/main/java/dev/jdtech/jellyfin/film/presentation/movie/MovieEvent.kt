package dev.jdtech.jellyfin.film.presentation.movie

sealed interface MovieEvent {
    data object ItemDeleted : MovieEvent

    data object DeleteFailed : MovieEvent
}
