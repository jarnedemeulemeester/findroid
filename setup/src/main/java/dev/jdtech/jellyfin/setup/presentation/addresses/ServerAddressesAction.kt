package dev.jdtech.jellyfin.setup.presentation.addresses

import java.util.UUID

sealed interface ServerAddressesAction {
    data class OnServerClick(val addressId: UUID) : ServerAddressesAction
    data class DeleteAddress(val addressId: UUID) : ServerAddressesAction
    data object OnAddClick : ServerAddressesAction
    data object OnBackClick : ServerAddressesAction
}
