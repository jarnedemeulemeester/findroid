package dev.jdtech.jellyfin.player.core.domain

import androidx.media3.common.C
import dev.jdtech.jellyfin.player.core.domain.models.PlaybackStatus
import dev.jdtech.jellyfin.repository.JellyfinRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for reporting playback events to the Jellyfin server.
 * Ensures consistent data formatting and error handling across different player implementations.
 */
@Singleton
class PlaybackManager @Inject constructor(
    private val repository: JellyfinRepository
) {
    /**
     * Reports that playback has started for a specific item.
     *
     * @param status The current [PlaybackStatus] containing item and session details.
     */
    suspend fun reportStart(status: PlaybackStatus) {
        if (status.itemId == null) {
            Timber.w("Skipping playback start report: itemId is null.")
            return
        }

        try {
            repository.postPlaybackStart(
                itemId = status.itemId,
                playMethod = status.playMethod,
                mediaSourceId = status.mediaSourceId,
                playSessionId = status.playSessionId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback start for item: ${status.itemId}")
        }
    }

    /**
     * Reports the current playback progress (position and pause state).
     *
     * @param status The current [PlaybackStatus] containing position and playback state.
     */
    suspend fun reportProgress(status: PlaybackStatus) {
        if (status.itemId == null) {
            Timber.w("Skipping playback progress report: itemId is null.")
            return
        }

        try {
            repository.postPlaybackProgress(
                itemId = status.itemId,
                positionTicks = status.positionMs * 10000, // Convert ms to ticks (1 tick = 100 nanoseconds)
                isPaused = status.isPaused,
                playMethod = status.playMethod,
                mediaSourceId = status.mediaSourceId,
                playSessionId = status.playSessionId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback progress for item: ${status.itemId}")
        }
    }

    /**
     * Reports that playback has stopped.
     * Calculates the played percentage based on current position and total duration.
     *
     * @param status The current [PlaybackStatus] containing final position and duration.
     */
    suspend fun reportStop(status: PlaybackStatus) {
        if (status.itemId == null) {
            Timber.w("Skipping playback stop report: itemId is null.")
            return
        }
        if (status.durationMs == C.TIME_UNSET) {
            Timber.w("Skipping playback stop report for item ${status.itemId}: invalid duration.")
            return
        }

        val positionTicks = status.positionMs * 10000 // Convert ms to ticks (1 tick = 100 nanoseconds)
        val playedPercentage = (status.positionMs.toFloat() / status.durationMs.toFloat() * 100).toInt().coerceIn(0, 100)

        try {
            repository.postPlaybackStop(
                itemId = status.itemId,
                positionTicks = positionTicks,
                playedPercentage = playedPercentage,
                mediaSourceId = status.mediaSourceId,
                playSessionId = status.playSessionId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback stop for item: ${status.itemId}")
        }
    }
}
