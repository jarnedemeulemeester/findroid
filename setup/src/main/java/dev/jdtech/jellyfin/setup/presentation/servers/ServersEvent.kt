package dev.jdtech.jellyfin.setup.presentation.servers

sealed interface ServersEvent {
    data object NavigateToUsers : ServersEvent
    data object NavigateToLogin : ServersEvent
}
