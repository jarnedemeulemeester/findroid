package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.ramcosta.composedestinations.annotation.ActivityDestination
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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

        setContent {
            FindroidTheme {
                val viewModel = hiltViewModel<PlayerActivityViewModel>()

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

                AndroidView(
                    factory = { context ->
                        PlayerView(context).also { playerView ->
                            playerView.player = viewModel.player
                            viewModel.initializePlayer(args.items.toTypedArray())
                            lifecycleOwner.lifecycle.coroutineScope.launch {
                                activity.keyDownEvents.receiveAsFlow().collect { keyEvent ->
                                    playerView.dispatchMediaKeyEvent(keyEvent)
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
            }
        }
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
}
