package dev.jdtech.jellyfin.repository

internal data class PlaybackStateUpdate(
    val played: Boolean,
    val playbackPositionTicks: Long,
)

internal fun playbackStopState(
    positionTicks: Long,
    playedPercentage: Int,
): PlaybackStateUpdate =
    when {
        playedPercentage < 10 ->
            PlaybackStateUpdate(played = false, playbackPositionTicks = 0)
        playedPercentage > 90 ->
            PlaybackStateUpdate(played = true, playbackPositionTicks = 0)
        else -> PlaybackStateUpdate(played = false, playbackPositionTicks = positionTicks)
    }

internal fun playbackProgressState(positionTicks: Long): PlaybackStateUpdate =
    PlaybackStateUpdate(played = false, playbackPositionTicks = positionTicks)
