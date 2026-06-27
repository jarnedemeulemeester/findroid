package dev.jdtech.jellyfin.player.core.domain.models

import androidx.media3.common.C
import org.jellyfin.sdk.model.api.PlayMethod
import java.util.UUID

/**
 * Data class representing the current state of media playback.
 * Used to encapsulate all necessary information when reporting playback events to the Jellyfin server.
 */
data class PlaybackStatus(
    /** The unique identifier of the item being played. */
    val itemId: UUID?,
    /** The current playback position in milliseconds. */
    val positionMs: Long,
    /** The total duration of the media in milliseconds. */
    val durationMs: Long = C.TIME_UNSET,
    /** Whether the playback is currently paused. */
    val isPaused: Boolean = false,
    /** The method used for playback (e.g., DirectPlay, Transcode). */
    val playMethod: PlayMethod = PlayMethod.DIRECT_PLAY,
    /** The identifier of the media source being streamed. */
    val mediaSourceId: String? = null,
    /** The unique identifier for the current playback session. */
    val playSessionId: String? = null
)
