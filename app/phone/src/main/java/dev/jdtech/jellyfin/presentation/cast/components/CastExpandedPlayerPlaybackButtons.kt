package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.models.FindroidSegmentType
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.player.core.R
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR


@Composable
fun PlaybackButtons(
    uiState: CastPlayerViewModel.UiState,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSkipSegment: () -> Unit
) {
    val playerState = uiState.playerState
    val skippableSegment = uiState.currentSegment != null
    val skipStringRes = uiState.currentSkipButtonStringRes

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        val backInteractionSource = remember { MutableInteractionSource() }
        val nextInteractionSource = remember { MutableInteractionSource() }
        val playPauseInteractionSource = remember { MutableInteractionSource() }

        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
        val isBackPressed by backInteractionSource.collectIsPressedAsState()
        val isNextPressed by nextInteractionSource.collectIsPressedAsState()

        val playPauseWeight by animateFloatAsState(
            targetValue = if (isPlayPausePressed && !skippableSegment) 1.7f
            else if (isPlayPausePressed && skippableSegment) 0.65f
            else if (isBackPressed && !skippableSegment) 1.1f
            else if (isBackPressed && skippableSegment) 0.25f
            else if (isNextPressed && !skippableSegment) 1.1f
            else if (isNextPressed && skippableSegment) 0.25f
            else if (skippableSegment) 0.45f
            else 1.3f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f
            ),
            label = "playPauseWeight",
        )

        val backButtonWeight by animateFloatAsState(
            targetValue = if (isBackPressed) 0.65f
            else if (isPlayPausePressed && !skippableSegment) 0.25f
            else if (isPlayPausePressed && skippableSegment) 0.35f
            else 0.45f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f
            ),
            label = "backButtonWeight",
        )

        val nextButtonWeight by animateFloatAsState(
            targetValue = if (isNextPressed && !skippableSegment) 0.65f
            else if (isNextPressed && skippableSegment) 1.5f
            else if (isPlayPausePressed && !skippableSegment) 0.25f
            else if (isPlayPausePressed && skippableSegment) 1.2f
            else if (skippableSegment) 1.3f
            else 0.45f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f
            ),
            label = "nextButtonWeight",
        )

        FilledIconButton(
            onClick = onSeekBack,
            enabled = uiState.fileLoaded,
            shape = CircleShape,
            interactionSource = backInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .height(68.dp)
                .weight(backButtonWeight),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_skip_back),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = { if (isPlaying) onPause() else onPlay() },
            enabled = uiState.fileLoaded,
            shape = CircleShape,
            interactionSource = playPauseInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier
                .height(68.dp)
                .weight(playPauseWeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) CoreR.drawable.ic_pause else CoreR.drawable.ic_play),
                    contentDescription = stringResource(if (isPlaying) R.string.player_controls_pause else R.string.player_controls_play),
                    modifier = Modifier.size(32.dp),
                )
                if (!skippableSegment) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (isPlaying) R.string.player_controls_pause else R.string.player_controls_play),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = { if (skippableSegment) onSkipSegment() else onSeekForward() },
            enabled = uiState.fileLoaded && (skippableSegment || playerState.hasNextItem),
            shape = CircleShape,
            interactionSource = nextInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (skippableSegment) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (skippableSegment) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .height(68.dp)
                .weight(nextButtonWeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (skippableSegment) Modifier.padding(horizontal = 16.dp) else Modifier
            ) {
                AnimatedVisibility(
                    visible = skippableSegment,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(skipStringRes),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
                Icon(
                    painter = painterResource(CoreR.drawable.ic_skip_forward),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaybackButtonsPreview() {
    FindroidTheme {
        Column {
            PlaybackButtons(
                uiState = mockUiState(),
                isPlaying = true,
                onPlay = {},
                onPause = {},
                onSeekBack = {},
                onSeekForward = {},
                onSkipSegment = {},
            )

            Spacer(Modifier.height(16.dp))

            PlaybackButtons(
                uiState = mockUiState(),
                isPlaying = false,
                onPlay = {},
                onPause = {},
                onSeekBack = {},
                onSeekForward = {},
                onSkipSegment = {},
            )

            Spacer(Modifier.height(16.dp))

            PlaybackButtons(
                uiState = mockUiState().copy(
                    currentSegment = FindroidSegment(
                        FindroidSegmentType.INTRO,
                        0,
                        1,
                    ),
                ),
                isPlaying = true,
                onPlay = {},
                onPause = {},
                onSeekBack = {},
                onSeekForward = {},
                onSkipSegment = {},
            )
        }
    }
}

private fun mockUiState() = CastPlayerViewModel.UiState(
    currentItemTitle = CastPlayerViewModel.CurrentItemTitle(
        seriesName = "Series Name", episodeInfo = "S01E01", title = "Episode Title"
    ),
    currentItemPosterUrl = null,
    isMovie = false,
    defaultAspectRatio = 16f / 9f,
    trickplayAspectRatio = null,
    currentSegment = null,
    currentSkipButtonStringRes = R.string.player_controls_skip_intro,
    currentTrickplay = null,
    currentChapters = emptyList(),
    fileLoaded = true
)
