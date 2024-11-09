package dev.jdtech.jellyfin.setup.presentation.discoverserver

sealed interface DiscoverServerEvent {
    data object Success : DiscoverServerEvent
}
