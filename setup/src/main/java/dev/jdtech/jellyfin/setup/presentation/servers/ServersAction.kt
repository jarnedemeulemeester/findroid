package dev.jdtech.jellyfin.setup.presentation.servers

import java.util.UUID

sealed interface ServersAction {
    data class OnServerClick(val serverId: String) : ServersAction
    data class OnAddressClick(val addressId: UUID) : ServersAction
    data class NavigateToAddresses(val serverId: String) : ServersAction
    data class DeleteServer(val serverId: String) : ServersAction
    data object OnAddClick : ServersAction
    data object OnBackClick : ServersAction
}
