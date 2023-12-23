package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ramcosta.composedestinations.annotation.ActivityDestination
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerControlsLayout
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerMediaTitle
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerOverlay
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerSeeker
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerState
import dev.jdtech.jellyfin.ui.components.player.rememberVideoPlayerState
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.utils.handleDPadKeyEvents
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PlayerActivityNavArgs(
    val items: ArrayList<PlayerItem>,
)

@AndroidEntryPoint
@ActivityDestination(
    navArgsDelegate = PlayerActivityNavArgs::class,
)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private val keyDownEvents = Channel<KeyEvent>()

    private val activity = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PlayerActivityDestination.argsFrom(intent)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            FindroidTheme {
                val viewModel = hiltViewModel<PlayerActivityViewModel>()

                val uiState by viewModel.uiState.collectAsState()

                var lifecycle by remember {
                    mutableStateOf(Lifecycle.Event.ON_CREATE)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        lifecycle = event
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
                                viewModel.initializePlayer(args.items.toTypedArray())
                                playerView.setBackgroundColor(
                                    resources.getColor(
                                        android.R.color.black,
                                        theme,
                                    ),
                                )
                                lifecycleOwner.lifecycle.coroutineScope.launch {
                                    activity.keyDownEvents.receiveAsFlow().collect { keyEvent ->
                                        // playerView.dispatchMediaKeyEvent(keyEvent)
                                    }
                                }
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
                            )
                        },
                    )
                }
            }
        }
    }

    @Composable
    fun VideoPlayerControls(
        title: String,
        isPlaying: Boolean,
        contentCurrentPosition: Long,
        player: Player,
        state: VideoPlayerState,
        focusRequester: FocusRequester,
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
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            when (it.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                -> {
                    lifecycleScope.launch {
                        keyDownEvents.send(it)
                    }
                    return true
                }
                else -> {}
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun Modifier.dPadEvents(
        exoPlayer: Player,
        videoPlayerState: VideoPlayerState,
        // pulseState: VideoPlayerPulseState
    ): Modifier = this.handleDPadKeyEvents(
        onLeft = {
            exoPlayer.seekBack()
            // pulseState.setType(BACK)
        },
        onRight = {
            exoPlayer.seekForward()
            // pulseState.setType(FORWARD)
        },
        onUp = { videoPlayerState.showControls() },
        onDown = { videoPlayerState.showControls() },
        onEnter = {
            exoPlayer.pause()
            videoPlayerState.showControls()
        },
    )
}
