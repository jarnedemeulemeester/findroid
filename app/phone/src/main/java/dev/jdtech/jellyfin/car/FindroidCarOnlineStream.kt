package dev.jdtech.jellyfin.car

import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType

internal fun FindroidCarCatalogItem.playbackSourceKind(): FindroidCarPlaybackSourceKind =
    if (videoPath.isNullOrBlank()) FindroidCarPlaybackSourceKind.ONLINE
    else FindroidCarPlaybackSourceKind.LOCAL

internal fun List<FindroidSource>.findroidStandardPlaybackSource(): FindroidSource? =
    firstOrNull { it.type == FindroidSourceType.LOCAL && it.path.isNotBlank() }
        ?: firstOrNull { it.path.isNotBlank() }

internal object FindroidCarPlaybackTimeline {
    fun absolutePositionMs(
        playerPositionMs: Long,
        streamStartPositionMs: Long,
        durationMs: Long,
    ): Long {
        val position = streamStartPositionMs.coerceAtLeast(0L) + playerPositionMs.coerceAtLeast(0L)
        return if (durationMs > 0L) position.coerceIn(0L, durationMs) else position
    }
}

internal fun findroidCarStartPositionMs(playbackPositionTicks: Long): Long =
    playbackPositionTicks.toMillis().takeIf { it > FINDROID_CAR_RESUME_THRESHOLD_MS } ?: 0L

internal enum class FindroidCarPlaybackSourceKind {
    LOCAL,
    ONLINE,
}

private const val FINDROID_CAR_TICKS_PER_MS = 10_000L
private const val FINDROID_CAR_RESUME_THRESHOLD_MS = 10_000L

private fun Long.toMillis(): Long = this / FINDROID_CAR_TICKS_PER_MS
