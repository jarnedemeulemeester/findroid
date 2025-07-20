package dev.jdtech.jellyfin.setup.presentation.servers

sealed interface ServersEvent {
    data object ServerChanged : ServersEvent
    data object AddressChanged : ServersEvent
}
