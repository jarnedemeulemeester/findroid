package dev.jdtech.jellyfin.setup.presentation.addresses

import dev.jdtech.jellyfin.models.ServerAddress

data class ServerAddressesState(
    val addresses: List<ServerAddress> = emptyList(),
)
