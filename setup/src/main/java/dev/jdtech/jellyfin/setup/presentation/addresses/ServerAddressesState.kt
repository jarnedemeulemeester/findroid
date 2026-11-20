package dev.jdtech.jellyfin.setup.presentation.addresses

import dev.jdtech.jellyfin.models.ServerAddress
import java.util.UUID

data class ServerAddressesState(
    val addresses: List<ServerAddress> = emptyList(),
    val currentAddressId: UUID? = null,
    val latencies: Map<UUID, Long?> = emptyMap(),
    val isMeasuring: Boolean = false,
    val autoSelectFastest: Boolean = true,
)
