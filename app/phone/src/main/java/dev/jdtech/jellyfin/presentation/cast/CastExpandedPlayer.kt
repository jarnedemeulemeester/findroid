package dev.jdtech.jellyfin.presentation.cast

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.C
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.cast.CastPlaybackState
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.presentation.cast.components.CastBottomControls
import dev.jdtech.jellyfin.presentation.cast.components.CastExpandedPlayerHeader
import dev.jdtech.jellyfin.presentation.cast.components.CastScrubbingTimeline
import dev.jdtech.jellyfin.presentation.cast.components.CastTrackSelectionSheet
import dev.jdtech.jellyfin.presentation.cast.components.PlaybackControls
import dev.jdtech.jellyfin.presentation.cast.components.TrickplayThumbnail
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastExpandedPlayer(
    castManager: CastManager,
    isExpandedScreen: Boolean, // true for tablets
    onDeviceClick: () -> Unit,
    onClose: () -> Unit,
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
    onClose: () -> Unit,
    viewModel: CastPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val playbackState by castManager.playbackState.collectAsState()
    val connectedDevice by castManager.connectedDevice.collectAsState()
    val volume by castManager.volume.collectAsState()

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var showTrackSelection by remember { mutableStateOf(false) }
    var trackType by remember { mutableIntStateOf(C.TRACK_TYPE_AUDIO) }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )

    val posterUrl = uiState.currentItemPosterUrl
    val segment = uiState.currentSegment
    val chapters = uiState.currentChapters

    Box(modifier = Modifier.fillMaxSize()) {
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
                onDeviceClick = onDeviceClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape && !isExpandedScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Side: Image
                    Box(modifier = Modifier.weight(1f)) {
                        PlayerImage(
                            uiState = uiState,
                            isScrubbing = isScrubbing,
                            scrubPosition = scrubPosition,
                            posterUrl = posterUrl,
                            playbackState = playbackState
                        )
                    }

                    // Right Side: Controls
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlayerTitles(uiState = uiState)
                        Spacer(modifier = Modifier.height(24.dp))
                        PlayerScrubber(
                            isScrubbing = isScrubbing,
                            scrubPosition = scrubPosition,
                            playbackState = playbackState,
                            chapters = chapters,
                            onScrubStart = { isScrubbing = true; scrubPosition = it },
                            onScrubStop = { isScrubbing = false; castManager.seekTo(scrubPosition.toLong()) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PlaybackControls(
                            isPlaying = playbackState.isPlaying,
                            onPlay = { castManager.play() },
                            onPause = { castManager.pause() },
                            onSeekBack = { viewModel.playPreviousItem() },
                            onSeekForward = { viewModel.playNextItem() },
                            onSkipSegment = { if (segment != null) viewModel.skipSegment(segment) },
                            skippableSegment = segment != null,
                            skipStringRes = uiState.currentSkipButtonStringRes,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CastBottomControls(
                            volume = volume,
                            onVolumeChange = { castManager.setVolume(it) },
                            onClickAudio = { showTrackSelection = true; trackType = C.TRACK_TYPE_AUDIO },
                            onClickSubtitle = { showTrackSelection = true; trackType = C.TRACK_TYPE_TEXT }
                        )
                    }
                }
            } else {
                // Portrait Layout
                PlayerImage(
                    uiState = uiState,
                    isScrubbing = isScrubbing,
                    scrubPosition = scrubPosition,
                    posterUrl = posterUrl,
                    playbackState = playbackState,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))
                PlayerTitles(uiState = uiState)
                Spacer(modifier = Modifier.height(24.dp))
                PlayerScrubber(
                    isScrubbing = isScrubbing,
                    scrubPosition = scrubPosition,
                    playbackState = playbackState,
                    chapters = chapters,
                    onScrubStart = { isScrubbing = true; scrubPosition = it },
                    onScrubStop = { isScrubbing = false; castManager.seekTo(scrubPosition.toLong()) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PlaybackControls(
                    isPlaying = playbackState.isPlaying,
                    onPlay = { castManager.play() },
                    onPause = { castManager.pause() },
                    onSeekBack = { viewModel.playPreviousItem() },
                    onSeekForward = { viewModel.playNextItem() },
                    onSkipSegment = { if (segment != null) viewModel.skipSegment(segment) },
                    skippableSegment = segment != null,
                    skipStringRes = uiState.currentSkipButtonStringRes,
                )
                Spacer(modifier = Modifier.height(24.dp))
                CastBottomControls(
                    volume = volume,
                    onVolumeChange = { castManager.setVolume(it) },
                    onClickAudio = { showTrackSelection = true; trackType = C.TRACK_TYPE_AUDIO },
                    onClickSubtitle = { showTrackSelection = true; trackType = C.TRACK_TYPE_TEXT }
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        AnimatedVisibility(
            visible = showTrackSelection,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { showTrackSelection = false }
                    }
            )
        }

        AnimatedVisibility(
            visible = showTrackSelection,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CastTrackSelectionSheet(
                type = trackType,
                tracks = if (trackType == C.TRACK_TYPE_AUDIO) uiState.audioTracks else uiState.subtitleTracks,
                onSetTrack = { if (trackType == C.TRACK_TYPE_AUDIO) viewModel.onAudioTrackSelected(it) else viewModel.onSubtitleTrackSelected(it) },
                onDismiss = { showTrackSelection = false },
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    }
            )
        }
    }
}

@Composable
private fun PlayerImage(
    uiState: CastPlayerViewModel.UiState,
    isScrubbing: Boolean,
    scrubPosition: Float,
    posterUrl: String?,
    playbackState: CastPlaybackState,
    modifier: Modifier = Modifier
) {
    val aspectRatio =
        if (isScrubbing && uiState.trickplayAspectRatio != null && uiState.currentTrickplay != null) {
            uiState.trickplayAspectRatio!!
        } else {
            uiState.defaultAspectRatio
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (uiState.isMovie && (!isScrubbing || uiState.currentTrickplay == null)) 88.dp else 24.dp)
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        if (isScrubbing && uiState.currentTrickplay != null && playbackState.duration > 0 && uiState.currentTrickplay!!.images.isNotEmpty()) {
            TrickplayThumbnail(
                trickplay = uiState.currentTrickplay!!,
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
            Icon(
                painterResource(CoreR.drawable.ic_cast),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PlayerTitles(uiState: CastPlayerViewModel.UiState) {
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

@Composable
private fun PlayerScrubber(
    isScrubbing: Boolean,
    scrubPosition: Float,
    playbackState: CastPlaybackState,
    chapters: List<PlayerChapter>,
    onScrubStart: (Float) -> Unit,
    onScrubStop: () -> Unit
) {
    val currentProgress =
        if (isScrubbing) scrubPosition else playbackState.currentPosition.toFloat()
    val duration = playbackState.duration.toFloat()

    CastScrubbingTimeline(
        currentProgress = currentProgress,
        duration = duration,
        chapters = chapters,
        onScrubStart = onScrubStart,
        onScrubStop = onScrubStop
    )
}
