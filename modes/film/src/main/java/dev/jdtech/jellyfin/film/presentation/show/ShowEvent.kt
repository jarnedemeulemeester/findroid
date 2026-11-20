package dev.jdtech.jellyfin.film.presentation.show

sealed interface ShowEvent {
    data class EpisodesDeleted(val deletedCount: Int, val requestedCount: Int) : ShowEvent

    data object EpisodesLoadFailed : ShowEvent

    data object DeleteFailed : ShowEvent
}
