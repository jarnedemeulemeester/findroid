package dev.jdtech.jellyfin.player.core.domain.utils

import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.models.FindroidSegmentType
import dev.jdtech.jellyfin.player.core.R

object SegmentUtils {
    fun shouldSkipToNextEpisode(
        segment: FindroidSegment,
        hasNextMediaItem: Boolean,
        playerDurationMillis: Long,
        nextEpisodeThreshold: Long
    ): Boolean {
        return if (segment.type == FindroidSegmentType.OUTRO && hasNextMediaItem) {
            val segmentEndTimeMillis = segment.endTicks
            val thresholdMillis = playerDurationMillis - nextEpisodeThreshold
            segmentEndTimeMillis > thresholdMillis
        } else {
            false
        }
    }

    fun getSkipButtonTextStringId(segment: FindroidSegment, shouldSkipToNextEpisode: Boolean): Int {
        return when (shouldSkipToNextEpisode) {
            true -> R.string.player_controls_next_episode
            false ->
                when (segment.type) {
                    FindroidSegmentType.INTRO -> R.string.player_controls_skip_intro
                    FindroidSegmentType.OUTRO -> R.string.player_controls_skip_outro
                    FindroidSegmentType.RECAP -> R.string.player_controls_skip_recap
                    FindroidSegmentType.COMMERCIAL -> R.string.player_controls_skip_commercial
                    FindroidSegmentType.PREVIEW -> R.string.player_controls_skip_preview
                    else -> R.string.player_controls_skip_unknown
                }
        }
    }
}
