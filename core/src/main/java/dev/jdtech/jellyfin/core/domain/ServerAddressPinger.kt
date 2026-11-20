package dev.jdtech.jellyfin.core.domain

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Measures how long it takes to reach a Jellyfin server address.
 *
 * The public system info endpoint is used because it does not require authentication and includes
 * the TCP, TLS and first response round trips.
 */
@Singleton
class ServerAddressPinger @Inject constructor() {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()

    /**
     * Ping a single address.
     *
     * @param [address] the server address, including scheme
     * @return the round trip time in milliseconds or null when the address is unreachable
     */
    suspend fun ping(address: String): Long? =
        withContext(Dispatchers.IO) {
            val request =
                try {
                    Request.Builder().url(address.trimEnd('/') + SYSTEM_INFO_PATH).get().build()
                } catch (e: IllegalArgumentException) {
                    Timber.d(e, "Invalid server address: $address")
                    return@withContext null
                }

            val start = System.nanoTime()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    (System.nanoTime() - start) / 1_000_000
                }
            } catch (e: Exception) {
                Timber.d(e, "Failed to ping $address")
                null
            }
        }

    /**
     * Ping all addresses in parallel.
     *
     * @return a map of address to round trip time in milliseconds, null when unreachable
     */
    suspend fun pingAll(addresses: List<String>): Map<String, Long?> =
        coroutineScope {
            addresses
                .distinct()
                .map { address -> async { address to ping(address) } }
                .awaitAll()
                .toMap()
        }

    companion object {
        private const val PING_TIMEOUT_MS = 1_200L
        private const val SYSTEM_INFO_PATH = "/System/Info/Public"
    }
}
