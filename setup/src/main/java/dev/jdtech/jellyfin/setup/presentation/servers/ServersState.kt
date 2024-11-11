package dev.jdtech.jellyfin.setup.presentation.servers

import dev.jdtech.jellyfin.models.Server

data class ServersState(
    val servers: List<Server> = emptyList(),
)
