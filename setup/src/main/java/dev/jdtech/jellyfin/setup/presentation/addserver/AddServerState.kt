package dev.jdtech.jellyfin.setup.presentation.addserver

import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.models.UiText

data class AddServerState(
    val isLoading: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val error: Collection<UiText>? = null,
)
