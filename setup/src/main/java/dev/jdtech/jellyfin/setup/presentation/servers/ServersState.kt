package dev.jdtech.jellyfin.setup.presentation.servers

import dev.jdtech.jellyfin.models.ServerWithAddresses

data class ServersState(
    val servers: List<ServerWithAddresses> = emptyList(),
)
