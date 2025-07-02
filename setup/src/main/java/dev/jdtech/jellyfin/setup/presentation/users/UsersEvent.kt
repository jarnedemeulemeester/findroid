package dev.jdtech.jellyfin.setup.presentation.users

sealed interface UsersEvent {
    data object NavigateToHome : UsersEvent
}
