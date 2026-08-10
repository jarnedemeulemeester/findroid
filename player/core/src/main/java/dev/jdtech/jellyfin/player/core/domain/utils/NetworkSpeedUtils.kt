package dev.jdtech.jellyfin.player.core.domain.utils

import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object NetworkSpeedUtils {
    private const val DEFAULT_TEST_SIZE = 512 * 1024 // 512KB
    private const val SAFETY_MARGIN = 0.8
    
    /**
     * Measures the network speed between the device and the Jellyfin server.
     *
     * @param api The Jellyfin API instance to use for the test.
     * @param size The amount of data to download for the test in bytes.
     * @return The suggested maximum bitrate in bps, or null if the test failed.
     */
    suspend fun measureNetworkSpeed(
        api: JellyfinApi,
        size: Int = DEFAULT_TEST_SIZE
    ): Int? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val response = api.mediaInfoApi.getBitrateTestBytes(size)
            val endTime = System.currentTimeMillis()
            val durationMs = endTime - startTime
            if (durationMs > 0) {
                val bytes = response.content.size
                
                // Calculate bits per second: (bytes * 8 bits/byte * 1000 ms/sec) / duration in ms
                val speedBps = (bytes.toLong() * 8 * 1000) / durationMs
                
                // Apply safety margin and ensure we don't overflow Int.MAX_VALUE
                val maxBitrate = (speedBps * SAFETY_MARGIN)
                    .toLong()
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()

                Timber.d("Measured network speed: ${speedBps / 1_000_000} Mbps")
                Timber.d("Suggested MaxBitrate: ${maxBitrate / 1_000_000} Mbps")

                maxBitrate
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure network speed")
            null
        }
    }
}