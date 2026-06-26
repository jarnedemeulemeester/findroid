package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackState
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter

@Composable
fun PlayerBottomSection(
    uiState: CastPlayerViewModel.UiState,
    playbackState: CastPlaybackState,
    chapters: List<PlayerChapter>,
    segment: FindroidSegment?,
    isScrubbing: Boolean,
    scrubPosition: Float,
    volume: Float,
    onScrubStart: (Float) -> Unit,
    onScrubStop: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPlayPreviousItem: () -> Unit,
    onPlayNextItem: () -> Unit,
    onSkipSegment: (FindroidSegment) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onClickAudio: () -> Unit,
    onClickSubtitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProgress = if (isScrubbing) scrubPosition else playbackState.currentPosition.toFloat()
    val duration = playbackState.duration.toFloat()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CastPlayerTitles(uiState = uiState)

        Spacer(modifier = Modifier.height(24.dp))

        CastScrubbingTimeline(
            currentProgress = currentProgress,
            duration = duration,
            chapters = chapters,
            onScrubStart = onScrubStart,
            onScrubStop = onScrubStop
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlaybackButtons(
            isPlaying = playbackState.isPlaying,
            onPlay = onPlay,
            onPause = onPause,
            onSeekBack = onPlayPreviousItem,
            onSeekForward = onPlayNextItem,
            onSkipSegment = { if (segment != null) onSkipSegment(segment) },
            skippableSegment = segment != null,
            skipStringRes = uiState.currentSkipButtonStringRes,
        )

        Spacer(modifier = Modifier.height(24.dp))

        CastTrackVolumeControls(
            volume = volume,
            onVolumeChange = onVolumeChange,
            onClickAudio = onClickAudio,
            onClickSubtitle = onClickSubtitle
        )
    }
}

@Composable
private fun CastPlayerTitles(uiState: CastPlayerViewModel.UiState) {
    val titleInfo = uiState.currentItemTitle
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (titleInfo.seriesName != null) {
            Text(
                text = titleInfo.seriesName!!,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = titleInfo.episodeInfo ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = titleInfo.title,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = titleInfo.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}