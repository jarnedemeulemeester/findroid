package dev.jdtech.jellyfin.presentation.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.presentation.cast.components.CastExpandedPlayerHeader
import dev.jdtech.jellyfin.presentation.cast.components.CastScrubbingTimeline
import dev.jdtech.jellyfin.presentation.cast.components.CastTrackSelectionDialog
import dev.jdtech.jellyfin.presentation.cast.components.CastVolumeControls
import dev.jdtech.jellyfin.presentation.cast.components.PlaybackControls
import dev.jdtech.jellyfin.presentation.cast.components.TrickplayThumbnail
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastExpandedPlayer(
    castManager: CastManager,
    isExpandedScreen: Boolean, // true for tablets
    onDeviceClick: () -> Unit,
    onClose: () -> Unit
) {
    if (!isExpandedScreen) {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RectangleShape,
            sheetMaxWidth = Dp.Unspecified
        ) {
            CastExpandedPlayerContent(
                castManager,
                onDeviceClick,
                onClose
            )
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.background
        ) {
            CastExpandedPlayerContent(
                castManager,
                onDeviceClick,
                onClose
            )
        }
    }
}

@Composable
private fun CastExpandedPlayerContent(
    castManager: CastManager,
    onDeviceClick: () -> Unit,
    onClose: () -> Unit
) {
    val viewModel = hiltViewModel<CastPlayerViewModel>()

    val uiState by viewModel.uiState.collectAsState()

    val playbackState by castManager.playbackState.collectAsState()
    val audioTracks by castManager.audioTracks.collectAsState()
    val subtitleTracks by castManager.subtitleTracks.collectAsState()
    val connectedDevice by castManager.connectedDevice.collectAsState()
    val volume by castManager.volume.collectAsState()

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var showTrackSelection by remember { mutableStateOf(false) }

    val titleInfo = uiState.currentItemTitle
    val posterUrl = uiState.currentItemPosterUrl
    val trickplay = uiState.currentTrickplay
    val segment = uiState.currentSegment
    val chapters = uiState.currentChapters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        CastExpandedPlayerHeader(
            deviceName = connectedDevice?.name,
            onClose = onClose,
            onDeviceClick = onDeviceClick,
            onOptionsClick = { showTrackSelection = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Center Image
        val aspectRatio = if (isScrubbing && uiState.trickplayAspectRatio != null && trickplay != null) {
            uiState.trickplayAspectRatio!!
        } else {
            uiState.defaultAspectRatio
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = if (uiState.isMovie && (!isScrubbing || trickplay == null)) 88.dp else 24.dp)
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (isScrubbing && trickplay != null && playbackState.duration > 0 && trickplay.images.isNotEmpty()) {
                TrickplayThumbnail(
                    trickplay = trickplay,
                    scrubPosition = scrubPosition
                )
            } else if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(painterResource(CoreR.drawable.ic_cast), contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (titleInfo.seriesName != null) {
            // Episode layout
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
            // Film layout
            Text(
                text = titleInfo.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Bar
        val currentProgress = if (isScrubbing) scrubPosition else playbackState.currentPosition.toFloat()
        val duration = playbackState.duration.toFloat()

        CastScrubbingTimeline(
            currentProgress = currentProgress,
            duration = duration,
            chapters = chapters,
            onScrubStart = {
                isScrubbing = true
                scrubPosition = it
            },
            onScrubStop = {
                isScrubbing = false
                castManager.seekTo(scrubPosition.toLong())
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls
        PlaybackControls(
            isPlaying = playbackState.isPlaying,
            onPlay = { castManager.play() },
            onPause = { castManager.pause() },
            onSeekBack = {
                castManager.seekTo(
                    (playbackState.currentPosition - 30000).coerceAtLeast(
                        0
                    )
                )
            },
            onSeekForward = {
                castManager.seekTo(
                    (playbackState.currentPosition + 30000).coerceAtMost(
                        playbackState.duration
                    )
                )
            },
            onSkipSegment = { if (segment != null) viewModel.skipSegment(segment) },
            skippableSegment = segment != null,
            skipStringRes = uiState.currentSkipButtonStringRes,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Volume Controls
        CastVolumeControls(
            volume = volume,
            onVolumeChange = { castManager.setVolume(it) },
            onToggleMute = { if (volume > 0f) castManager.setVolume(0f) else castManager.setVolume(0.5f) }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showTrackSelection) {
        CastTrackSelectionDialog(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            onSetAudioTrack = { castManager.setAudioTrack(it) },
            onSetSubtitleTrack = { castManager.setSubtitleTrack(it) },
            onDismiss = { showTrackSelection = false }
        )
    }
}
