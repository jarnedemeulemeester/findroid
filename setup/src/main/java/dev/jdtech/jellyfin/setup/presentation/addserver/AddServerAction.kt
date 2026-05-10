package dev.jdtech.jellyfin.setup.presentation.addserver

sealed interface AddServerAction {
    data class OnConnectClick(val address: String) : AddServerAction
    data object RequestLocalNetworkAccessPermission : AddServerAction

    data object OnBackClick : AddServerAction
}
