package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.player.cast.CastDevice
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.cast.CastPlaybackState
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.PlayerMediaType
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastMiniPlayer(
    castManager: CastManager,
    currentItem: PlayerItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectedDevice by castManager.connectedDevice.collectAsState()
    val playbackState by castManager.playbackState.collectAsState()

    if (connectedDevice != null) {
        CastMiniPlayerLayout(
            connectedDevice = connectedDevice!!,
            currentItem = currentItem,
            playbackState = playbackState,
            onTogglePlayback = {
                if (playbackState.isPlaying) castManager.pause() else castManager.play()
            },
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
fun CastMiniPlayerLayout(
    connectedDevice: CastDevice,
    currentItem: PlayerItem?,
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
                            .fillMaxWidth(0.4f)
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
                    if (currentItem != null) {
                        AsyncImage(
                            model = currentItem.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(64.dp)
                                .aspectRatio(if (currentItem.mediaType == PlayerMediaType.EPISODE) 16f / 9f else 2f / 3f)
                                .clip(MaterialTheme.shapes.medium),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = currentItem.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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

                if (currentItem != null) {
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
            connectedDevice = CastDevice("1", "Living Room TV"),
            currentItem = null,
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
            connectedDevice = CastDevice("1", "Living Room TV"),
            currentItem = PlayerItem(
                name = "Interstellar",
                itemId = UUID.randomUUID(),
                mediaSourceId = "1",
                playbackPosition = 0L,
                posterUrl = null,
                mediaType = PlayerMediaType.MOVIE
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
            connectedDevice = CastDevice("1", "Living Room TV"),
            currentItem = PlayerItem(
                name = "The Hansboro Incident",
                itemId = UUID.randomUUID(),
                mediaSourceId = "1",
                playbackPosition = 0L,
                posterUrl = null,
                mediaType = PlayerMediaType.EPISODE
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
            connectedDevice = CastDevice("1", "Living Room TV"),
            currentItem = PlayerItem(
                name = "Interstellar",
                itemId = UUID.randomUUID(),
                mediaSourceId = "1",
                playbackPosition = 0L,
                posterUrl = null,
                mediaType = PlayerMediaType.MOVIE
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
            connectedDevice = CastDevice("1", "Living Room TV"),
            currentItem = PlayerItem(
                name = "The Hansboro Incident",
                itemId = UUID.randomUUID(),
                mediaSourceId = "1",
                playbackPosition = 0L,
                posterUrl = null,
                mediaType = PlayerMediaType.EPISODE
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
