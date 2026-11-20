package dev.jdtech.jellyfin.core.domain

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.ServerAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Picks the fastest reachable address of a server and stores it as the current address.
 */
@Singleton
class ServerAddressSelector
@Inject
constructor(
    private val database: ServerDatabaseDao,
    private val pinger: ServerAddressPinger,
) {
    /**
     * Ping all addresses of [serverId] and make the fastest one the current address.
     *
     * The current address is kept when the server only has one address, when none of the addresses
     * is reachable or when the current address is already the fastest one.
     *
     * @return the address that is (now) the current address of the server
     */
    suspend fun selectFastestAddress(serverId: String): ServerAddress? =
        withContext(Dispatchers.IO) {
            val serverWithAddresses = database.getServerWithAddresses(serverId)
            val addresses = serverWithAddresses.addresses
            val currentAddressId = serverWithAddresses.server.currentServerAddressId
            val currentAddress = addresses.firstOrNull { it.id == currentAddressId }

            if (addresses.size < 2) return@withContext currentAddress

            val latencies = pinger.pingAll(addresses.map { it.address })
            val fastestAddress =
                addresses
                    .filter { latencies[it.address] != null }
                    .minByOrNull { latencies[it.address] ?: Long.MAX_VALUE }
                    ?: return@withContext currentAddress

            if (fastestAddress.id != currentAddressId) {
                Timber.d(
                    "Switching to fastest server address ${fastestAddress.address} (${latencies[fastestAddress.address]} ms)"
                )
                database.updateServerCurrentAddress(serverId, fastestAddress.id)
            }

            fastestAddress
        }
}
