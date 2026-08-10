package dev.jdtech.jellyfin.presentation.cast

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.C
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.presentation.cast.components.CastTrackSelectionSheet
import dev.jdtech.jellyfin.presentation.cast.components.PlayerBottomSection
import dev.jdtech.jellyfin.presentation.cast.components.PlayerTopSection
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.player.core.R as PlayerCoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastExpandedPlayer(
    onDeviceClick: () -> Unit,
    onClose: () -> Unit,
    viewModel: CastPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )

    val actions = remember(viewModel, uiState.playerState.currentPosition) {
        CastPlayerActions(
            onPlay = { viewModel.playerController.play() },
            onPause = { viewModel.playerController.pause() },
            onSeek = { viewModel.playerController.seekTo(it) },
            onPlayPreviousItem = {
                if (!uiState.playerState.hasPreviousItem || uiState.playerState.currentPosition > 5000) {
                    viewModel.playerController.seekTo(0)
                } else {
                    viewModel.playPreviousItem()
                }
            },
            onPlayNextItem = { viewModel.playNextItem() },
            onSkipSegment = { viewModel.skipSegment(it) },
            onVolumeChange = { viewModel.playerController.setVolume(it) },
            onAudioTrackSelected = { viewModel.onAudioTrackSelected(it) },
            onSubtitleTrackSelected = { viewModel.onSubtitleTrackSelected(it) },
            onDeviceClick = onDeviceClick,
            onClose = onClose
        )
    }

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
            CastExpandedPlayerLayout(
                isExpandedScreen = false,
                uiState = uiState,
                actions = actions
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
            CastExpandedPlayerLayout(
                isExpandedScreen = true,
                uiState = uiState,
                actions = actions
            )
        }
    }
}

data class CastPlayerActions(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onPlayPreviousItem: () -> Unit,
    val onPlayNextItem: () -> Unit,
    val onSkipSegment: (FindroidSegment) -> Unit,
    val onVolumeChange: (Float) -> Unit,
    val onAudioTrackSelected: (Track) -> Unit,
    val onSubtitleTrackSelected: (Track?) -> Unit,
    val onDeviceClick: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
private fun CastExpandedPlayerLayout(
    isExpandedScreen: Boolean,
    uiState: CastPlayerViewModel.UiState,
    actions: CastPlayerActions,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var showTrackSelection by remember { mutableStateOf(false) }
    var trackType by remember { mutableIntStateOf(C.TRACK_TYPE_AUDIO) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val safePadding = rememberSafePadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = safePadding.top,
                    bottom = safePadding.bottom,
                    start = safePadding.start,
                    end = safePadding.end
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLandscape && !isExpandedScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerTopSection(
                        uiState = uiState,
                        isScrubbing = isScrubbing,
                        scrubPosition = scrubPosition,
                        onClose = actions.onClose,
                        onDeviceClick = actions.onDeviceClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    PlayerBottomSection(
                        uiState = uiState,
                        isScrubbing = isScrubbing,
                        scrubPosition = scrubPosition,
                        onScrubStart = {
                            isScrubbing = true
                            scrubPosition = it
                        },
                        onScrubStop = {
                            isScrubbing = false
                            actions.onSeek(scrubPosition.toLong())
                        },
                        onPlay = actions.onPlay,
                        onPause = actions.onPause,
                        onPlayPreviousItem = actions.onPlayPreviousItem,
                        onPlayNextItem = actions.onPlayNextItem,
                        onSkipSegment = actions.onSkipSegment,
                        onVolumeChange = actions.onVolumeChange,
                        onClickAudio = {
                            showTrackSelection = true
                            trackType = C.TRACK_TYPE_AUDIO
                        },
                        onClickSubtitle = {
                            showTrackSelection = true
                            trackType = C.TRACK_TYPE_TEXT
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                PlayerTopSection(
                    uiState = uiState,
                    isScrubbing = isScrubbing,
                    scrubPosition = scrubPosition,
                    onClose = actions.onClose,
                    onDeviceClick = actions.onDeviceClick,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                PlayerBottomSection(
                    uiState = uiState,
                    isScrubbing = isScrubbing,
                    scrubPosition = scrubPosition,
                    onScrubStart = {
                        isScrubbing = true
                        scrubPosition = it
                    },
                    onScrubStop = {
                        isScrubbing = false
                        actions.onSeek(scrubPosition.toLong())
                    },
                    onPlay = actions.onPlay,
                    onPause = actions.onPause,
                    onPlayPreviousItem = actions.onPlayPreviousItem,
                    onPlayNextItem = actions.onPlayNextItem,
                    onSkipSegment = actions.onSkipSegment,
                    onVolumeChange = actions.onVolumeChange,
                    onClickAudio = {
                        showTrackSelection = true
                        trackType = C.TRACK_TYPE_AUDIO
                    },
                    onClickSubtitle = {
                        showTrackSelection = true
                        trackType = C.TRACK_TYPE_TEXT
                    })
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        AnimatedVisibility(
            visible = showTrackSelection, enter = fadeIn(), exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { showTrackSelection = false }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 10f) showTrackSelection = false
                            })
                    })
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
                onSetTrack = {
                    if (trackType == C.TRACK_TYPE_AUDIO) {
                        it?.let { actions.onAudioTrackSelected(it) }
                    } else {
                        actions.onSubtitleTrackSelected(it)
                    }
                },
                onDismiss = { showTrackSelection = false },
                displayExtraInfo = uiState.displayExtraInfo,
                modifier = Modifier
                    .padding(MaterialTheme.spacings.small)
                    .widthIn(max = 500.dp)
                    .padding(bottom = safePadding.bottom)
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    })
        }
    }
}

@Preview(name = "Vertical", showBackground = true)
@Composable
private fun CastExpandedPlayerPhoneVerticalPreview() {
    FindroidTheme {
        CastExpandedPlayerLayout(
            isExpandedScreen = false,
            uiState = mockUiStateEpisode(),
            actions = mockActions()
        )
    }
}

@Preview(name = "Horizontal", showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun CastExpandedPlayerPhoneHorizontalPreview() {
    FindroidTheme {
        CastExpandedPlayerLayout(
            isExpandedScreen = false,
            uiState = mockUiStateEpisode(),
            actions = mockActions()
        )
    }
}

@Preview(name = "Vertical Movie", showBackground = true)
@Composable
private fun CastExpandedPlayerMoviePhoneVerticalPreview() {
    FindroidTheme {
        CastExpandedPlayerLayout(
            isExpandedScreen = false,
            uiState = mockUiStateMovie(),
            actions = mockActions()
        )
    }
}

@Preview(name = "Horizontal Movie", showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun CastExpandedPlayerMoviePhoneHorizontalPreview() {
    FindroidTheme {
        CastExpandedPlayerLayout(
            isExpandedScreen = false,
            uiState = mockUiStateMovie(),
            actions = mockActions()
        )
    }
}

private fun mockActions() = CastPlayerActions(
    onPlay = {},
    onPause = {},
    onSeek = {},
    onPlayPreviousItem = {},
    onPlayNextItem = {},
    onSkipSegment = {},
    onVolumeChange = {},
    onAudioTrackSelected = {},
    onSubtitleTrackSelected = {},
    onDeviceClick = {},
    onClose = {}
)

private fun mockUiStateEpisode() = CastPlayerViewModel.UiState(
    currentItemTitle = CastPlayerViewModel.CurrentItemTitle(
        seriesName = "Series Name", episodeInfo = "S01E01", title = "Episode Title"
    ),
    currentItemPosterUrl = null,
    isMovie = false,
    defaultAspectRatio = 16f / 9f,
    trickplayAspectRatio = null,
    currentSegment = null,
    currentSkipButtonStringRes = PlayerCoreR.string.player_controls_skip_intro,
    currentTrickplay = null,
    currentChapters = emptyList(),
    fileLoaded = true
)

private fun mockUiStateMovie() = CastPlayerViewModel.UiState(
    currentItemTitle = CastPlayerViewModel.CurrentItemTitle(
        title = "Title"
    ),
    currentItemPosterUrl = null,
    isMovie = true,
    defaultAspectRatio = 2f / 3f,
    trickplayAspectRatio = null,
    currentSegment = null,
    currentSkipButtonStringRes = PlayerCoreR.string.player_controls_skip_intro,
    currentTrickplay = null,
    currentChapters = emptyList(),
    fileLoaded = true
)
