package dev.jdtech.jellyfin.presentation.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.player.cast.models.Device
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackState
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastMiniPlayer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CastPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectedDevice = uiState.connectedDevice
    val playbackState = uiState.playbackState

    if (connectedDevice != null) {
        CastMiniPlayerLayout(
            connectedDevice = connectedDevice,
            uiState = uiState,
            playbackState = playbackState,
            onTogglePlayback = {
                if (playbackState.isPlaying) viewModel.playerController.pause() else viewModel.playerController.play()
            },
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
fun CastMiniPlayerLayout(
    connectedDevice: Device,
    uiState: CastPlayerViewModel.UiState,
    playbackState: CastPlaybackState,
    onTogglePlayback: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isExpandedScreen) Alignment.BottomEnd else Alignment.BottomCenter
    ) {
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(MaterialTheme.spacings.medium)
                .then(
                    if (isExpandedScreen) {
                        Modifier
                            .widthIn(max = 1000.dp)
                            .fillMaxWidth(0.5f)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .padding(MaterialTheme.spacings.small)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)
                ) {
                    if (uiState.fileLoaded) {
                        AsyncImage(
                            model = uiState.currentItemPosterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(if (isExpandedScreen) 80.dp else 64.dp)
                                .aspectRatio(uiState.defaultAspectRatio)
                                .clip(MaterialTheme.shapes.medium),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            val titleInfo = uiState.currentItemTitle
                            if (titleInfo.seriesName != null) {
                                // Episode layout
                                Text(
                                    text = titleInfo.seriesName!!,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = titleInfo.episodeInfo + " - " + titleInfo.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                // Film layout
                                Text(
                                    text = titleInfo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = connectedDevice.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = onTogglePlayback,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    painter = if (playbackState.isPlaying) painterResource(CoreR.drawable.ic_pause) else painterResource(
                                        CoreR.drawable.ic_play
                                    ),
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play"
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = MaterialTheme.spacings.small)
                        ) {
                            Text(
                                text = connectedDevice.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_cast),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (uiState.fileLoaded) {
                    LinearProgressIndicator(
                        progress = {
                            if (playbackState.duration > 0) {
                                playbackState.currentPosition.toFloat() / playbackState.duration
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        strokeCap = StrokeCap.Butt
                    )
                }
            }
        }
    }
}

@Preview(heightDp = 150)
@Composable
private fun CastMiniPlayerPhonePreview() {
    FindroidTheme {
        CastMiniPlayerLayout(
            connectedDevice = Device("1", "Living Room TV"),
            uiState = previewUiState(posterUrl = null),
            playbackState = CastPlaybackState(),
            onTogglePlayback = {},
            onClick = {}
        )
    }
}

@Preview(heightDp = 150)
@Composable
private fun CastMiniPlayerPlayingPhonePreview() {
    FindroidTheme {
        CastMiniPlayerLayout(
            connectedDevice = Device("1", "Living Room TV"),
            uiState = previewUiState(
                title = "Interstellar",
                isMovie = true,
                aspectRatio = 2f / 3f,
                fileLoaded = true,
            ),
            playbackState = CastPlaybackState(
                isPlaying = true,
                currentPosition = 5000L,
                duration = 10000L
            ),
            onTogglePlayback = {},
            onClick = {}
        )
    }
}

@Preview(heightDp = 150)
@Composable
private fun CastMiniPlayerPlayingEpisodePhonePreview() {
    FindroidTheme {
        CastMiniPlayerLayout(
            connectedDevice = Device("1", "Living Room TV"),
            uiState = previewUiState(
                title = "The Hansboro Incident",
                seriesName = "The Expanse",
                episodeInfo = "S01:E01",
                isMovie = false,
                aspectRatio = 16f / 9f,
                fileLoaded = true,
            ),
            playbackState = CastPlaybackState(
                isPlaying = true,
                currentPosition = 2500L,
                duration = 10000L
            ),
            onTogglePlayback = {},
            onClick = {}
        )
    }
}

@Preview(widthDp = 900, heightDp = 150)
@Composable
private fun CastMiniPlayerTabletPreview() {
    FindroidTheme {
        CastMiniPlayerLayout(
            connectedDevice = Device("1", "Living Room TV"),
            uiState = previewUiState(
                title = "Interstellar",
                isMovie = true,
                aspectRatio = 2f / 3f,
                fileLoaded = true,
            ),
            playbackState = CastPlaybackState(
                isPlaying = false,
                currentPosition = 3000L,
                duration = 10000L
            ),
            onTogglePlayback = {},
            onClick = {}
        )
    }
}

@Preview(widthDp = 900, heightDp = 150)
@Composable
private fun CastMiniPlayerEpisodeTabletPreview() {
    FindroidTheme {
        CastMiniPlayerLayout(
            connectedDevice = Device("1", "Living Room TV"),
            uiState = previewUiState(
                title = "The Hansboro Incident",
                seriesName = "The Expanse",
                episodeInfo = "S01:E01",
                isMovie = false,
                aspectRatio = 16f / 9f,
                fileLoaded = true,
            ),
            playbackState = CastPlaybackState(
                isPlaying = false,
                currentPosition = 5000L,
                duration = 10000L
            ),
            onTogglePlayback = {},
            onClick = {}
        )
    }
}

private fun previewUiState(
    title: String = "Title",
    seriesName: String? = null,
    episodeInfo: String? = null,
    posterUrl: String? = "url",
    isMovie: Boolean = true,
    aspectRatio: Float = 16f / 9f,
    fileLoaded: Boolean = false
) = CastPlayerViewModel.UiState(
    currentItemTitle = CastPlayerViewModel.CurrentItemTitle(
        seriesName = seriesName,
        episodeInfo = episodeInfo,
        title = title
    ),
    currentItemPosterUrl = posterUrl,
    isMovie = isMovie,
    defaultAspectRatio = aspectRatio,
    trickplayAspectRatio = null,
    currentSegment = null,
    currentSkipButtonStringRes = 0,
    currentTrickplay = null,
    currentChapters = emptyList(),
    fileLoaded = fileLoaded
)
