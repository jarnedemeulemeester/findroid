package dev.jdtech.jellyfin.setup.presentation.discoverserver

sealed interface DiscoverServerAction {
    data class OnServerClick(val address: String) : DiscoverServerAction
    data object OnManualClick : DiscoverServerAction
    data object OnBackClick : DiscoverServerAction
}
