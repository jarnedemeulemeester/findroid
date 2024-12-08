package dev.jdtech.jellyfin.setup.presentation.servers

sealed interface ServersAction {
    data class OnServerClick(val serverId: String) : ServersAction
    data class DeleteServer(val serverId: String) : ServersAction
    data object OnAddClick : ServersAction
    data object OnBackClick : ServersAction
}
