package dev.jdtech.jellyfin.film.presentation.home

sealed interface HomeAction {
    data object OnRetryClick : HomeAction
}
