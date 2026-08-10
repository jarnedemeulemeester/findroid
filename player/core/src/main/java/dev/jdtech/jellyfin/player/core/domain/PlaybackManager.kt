package dev.jdtech.jellyfin.player.core.domain

import androidx.media3.common.C
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.PlayMethod
import timber.log.Timber
import java.util.UUID
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
     * @param itemId The UUID of the item being played.
     * @param positionMs The starting playback position in milliseconds.
     * @param playMethod The [PlayMethod] used for playback (defaults to [PlayMethod.DIRECT_PLAY]).
     * @param mediaSourceId The ID of the media source (defaults to null).
     * @param playSessionId The ID of the play session (defaults to null).
     */
    suspend fun reportStart(
        itemId: UUID,
        positionMs: Long? = null,
        playMethod: PlayMethod = PlayMethod.DIRECT_PLAY,
        mediaSourceId: String? = null,
        playSessionId: String? = null
    ) {
        try {
            repository.postPlaybackStart(
                itemId = itemId,
                positionTicks = positionMs?.let { it * 10000 }, // Convert ms to ticks (1 tick = 100 nanoseconds)
                playMethod = playMethod,
                mediaSourceId = mediaSourceId,
                playSessionId = playSessionId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback start for item: $itemId")
        }
    }

    /**
     * Reports the current playback progress (position and pause state).
     *
     * @param itemId The UUID of the item being played.
     * @param positionMs The current playback position in milliseconds. Must be non-negative.
     * @param isPaused Whether the playback is currently paused (defaults to false).
     * @param playMethod The [PlayMethod] used for playback (defaults to [PlayMethod.DIRECT_PLAY]).
     * @param mediaSourceId The ID of the media source (defaults to null).
     * @param playSessionId The ID of the play session (defaults to null).
     */
    suspend fun reportProgress(
        itemId: UUID,
        positionMs: Long,
        isPaused: Boolean = false,
        playMethod: PlayMethod = PlayMethod.DIRECT_PLAY,
        mediaSourceId: String? = null,
        playSessionId: String? = null
    ) {
        require(positionMs >= 0) { "positionMs must be non-negative: $positionMs" }
        try {
            repository.postPlaybackProgress(
                itemId = itemId,
                positionTicks = positionMs * 10000, // Convert ms to ticks (1 tick = 100 nanoseconds)
                isPaused = isPaused,
                playMethod = playMethod,
                mediaSourceId = mediaSourceId,
                playSessionId = playSessionId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback progress for item: $itemId")
        }
    }

    /**
     * Reports that playback has stopped.
     * Calculates the played percentage based on current position and total duration.
     *
     * @param itemId The UUID of the item being played.
     * @param positionMs The final playback position in milliseconds. Must be non-negative.
     * @param durationMs The total duration of the item in milliseconds. Must be greater than 0.
     * @param mediaSourceId The ID of the media source (defaults to null).
     * @param playSessionId The ID of the play session (defaults to null).
     */
    suspend fun reportStop(
        itemId: UUID,
        positionMs: Long,
        durationMs: Long,
        mediaSourceId: String? = null,
        playSessionId: String? = null
    ) {
        require(positionMs >= 0) { "positionMs must be non-negative: $positionMs" }
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) {
            Timber.w("Skipping playback stop report for item ${itemId}: invalid duration.")
            return
        }

        val positionTicks = positionMs * 10000 // Convert ms to ticks (1 tick = 100 nanoseconds)
        val playedPercentage = (positionMs.toFloat() / durationMs.toFloat() * 100).toInt().coerceIn(0, 100)

        try {
            repository.postPlaybackStop(
                itemId = itemId,
                positionTicks = positionTicks,
                playedPercentage = playedPercentage,
                mediaSourceId = mediaSourceId,
                playSessionId = playSessionId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback stop for item: $itemId")
        }
    }
}
