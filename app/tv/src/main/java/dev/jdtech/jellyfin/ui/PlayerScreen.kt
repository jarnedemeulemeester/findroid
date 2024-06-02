package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.destinations.VideoPlayerTrackSelectorDialogDestination
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.Track
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerControlsLayout
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerMediaButton
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerMediaTitle
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerOverlay
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerSeeker
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerState
import dev.jdtech.jellyfin.ui.components.player.rememberVideoPlayerState
import dev.jdtech.jellyfin.ui.dialogs.VideoPlayerTrackSelectorDialogResult
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.handleDPadKeyEvents
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Destination
@Composable
fun PlayerScreen(
    navigator: DestinationsNavigator,
    items: ArrayList<PlayerItem>,
    resultRecipient: ResultRecipient<VideoPlayerTrackSelectorDialogDestination, VideoPlayerTrackSelectorDialogResult>,
) {
    val viewModel = hiltViewModel<PlayerActivityViewModel>()

    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    var mediaSession by remember {
        mutableStateOf<MediaSession?>(null)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event

            // Handle creation and release of media session
            when (lifecycle) {
                Lifecycle.Event.ON_STOP -> {
                    println("ON_STOP")
                    mediaSession?.release()
                }

                Lifecycle.Event.ON_START -> {
                    println("ON_START")
                    mediaSession = MediaSession.Builder(context, viewModel.player).build()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val videoPlayerState = rememberVideoPlayerState()

    var currentPosition by remember {
        mutableLongStateOf(0L)
    }
    var isPlaying by remember {
        mutableStateOf(viewModel.player.isPlaying)
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            currentPosition = viewModel.player.currentPosition
            isPlaying = viewModel.player.isPlaying
        }
    }

    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> Unit
            is NavResult.Value -> {
                val trackType = result.value.trackType
                val index = result.value.index

                if (index == -1) {
                    viewModel.player.trackSelectionParameters = viewModel.player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(trackType)
                        .setTrackTypeDisabled(trackType, true)
                        .build()
                } else {
                    viewModel.player.trackSelectionParameters = viewModel.player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(
                            TrackSelectionOverride(viewModel.player.currentTracks.groups[index].mediaTrackGroup, 0),
                        )
                        .setTrackTypeDisabled(trackType, false)
                        .build()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .dPadEvents(
                exoPlayer = viewModel.player,
                videoPlayerState = videoPlayerState,
            )
            .focusable(),
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).also { playerView ->
                    playerView.player = viewModel.player
                    playerView.useController = false
                    viewModel.initializePlayer(items.toTypedArray())
                    playerView.setBackgroundColor(
                        context.resources.getColor(
                            android.R.color.black,
                            context.theme,
                        ),
                    )
                }
            },
            update = {
                when (lifecycle) {
                    Lifecycle.Event.ON_PAUSE -> {
                        it.onPause()
                        it.player?.pause()
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        it.onResume()
                    }

                    else -> Unit
                }
            },
            modifier = Modifier
                .fillMaxSize(),
        )
        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = videoPlayerState,
            isPlaying = isPlaying,
            controls = {
                VideoPlayerControls(
                    title = uiState.currentItemTitle,
                    isPlaying = isPlaying,
                    contentCurrentPosition = currentPosition,
                    player = viewModel.player,
                    state = videoPlayerState,
                    focusRequester = focusRequester,
                    navigator = navigator,
                )
            },
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerControls(
    title: String,
    isPlaying: Boolean,
    contentCurrentPosition: Long,
    player: Player,
    state: VideoPlayerState,
    focusRequester: FocusRequester,
    navigator: DestinationsNavigator,
) {
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            player.play()
        } else {
            player.pause()
        }
    }

    VideoPlayerControlsLayout(
        mediaTitle = {
            VideoPlayerMediaTitle(
                title = title,
                subtitle = null,
            )
        },
        seeker = {
            VideoPlayerSeeker(
                focusRequester = focusRequester,
                state = state,
                isPlaying = isPlaying,
                onPlayPauseToggle = onPlayPauseToggle,
                onSeek = { player.seekTo(player.duration.times(it).toLong()) },
                contentProgress = contentCurrentPosition.milliseconds,
                contentDuration = player.duration.milliseconds,
            )
        },
        mediaActions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                VideoPlayerMediaButton(
                    icon = painterResource(id = R.drawable.ic_speaker),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        val tracks = getTracks(player, C.TRACK_TYPE_AUDIO)
                        navigator.navigate(VideoPlayerTrackSelectorDialogDestination(C.TRACK_TYPE_AUDIO, tracks))
                    },
                )
                VideoPlayerMediaButton(
                    icon = painterResource(id = R.drawable.ic_closed_caption),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        val tracks = getTracks(player, C.TRACK_TYPE_TEXT)
                        navigator.navigate(VideoPlayerTrackSelectorDialogDestination(C.TRACK_TYPE_TEXT, tracks))
                    },
                )
            }
        },
    )
}

private fun Modifier.dPadEvents(
    exoPlayer: Player,
    videoPlayerState: VideoPlayerState,
): Modifier = this.handleDPadKeyEvents(
    onLeft = {},
    onRight = {},
    onUp = {},
    onDown = {},
    onEnter = {
        exoPlayer.pause()
        videoPlayerState.showControls()
    },
)

@androidx.annotation.OptIn(UnstableApi::class)
private fun getTracks(player: Player, type: Int): Array<Track> {
    val tracks = arrayListOf<Track>()
    for (groupIndex in 0 until player.currentTracks.groups.count()) {
        val group = player.currentTracks.groups[groupIndex]
        if (group.type == type) {
            val format = group.mediaTrackGroup.getFormat(0)

            val track = Track(
                id = groupIndex,
                label = format.label,
                language = Locale(format.language.toString()).displayLanguage,
                codec = format.codecs,
                selected = group.isSelected,
                supported = group.isSupported,
            )

            tracks.add(track)
        }
    }

    val noneTrack = Track(
        id = -1,
        label = null,
        language = null,
        codec = null,
        selected = !tracks.any { it.selected },
        supported = true,
    )
    return arrayOf(noneTrack) + tracks
}
