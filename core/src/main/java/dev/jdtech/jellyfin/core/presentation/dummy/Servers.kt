package dev.jdtech.jellyfin.core.presentation.dummy

import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import java.util.UUID

val dummyDiscoveredServer = DiscoveredServer(
    id = "",
    name = "Demo server",
    address = "https://demo.jellyfin.org/stable",
)

val dummyServer = Server(
    id = "",
    name = "Demo server",
    currentServerAddressId = UUID.randomUUID(),
    currentUserId = UUID.randomUUID(),
)

val dummyServerAddress = ServerAddress(
    id = UUID.randomUUID(),
    address = "http://192.168.0.10:8096",
    serverId = "",
)
