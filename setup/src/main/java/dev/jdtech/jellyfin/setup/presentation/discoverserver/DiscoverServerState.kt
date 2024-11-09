package dev.jdtech.jellyfin.setup.presentation.discoverserver

import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.models.UiText

data class DiscoverServerState(
    val isLoading: Boolean = true,
    val servers: List<DiscoveredServer> = emptyList(),
    val error: Collection<UiText>? = null,
)
