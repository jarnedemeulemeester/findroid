package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.ActivityDestination
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.scope.resultRecipient
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.destinations.PlayerScreenDestination
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.ui.PlayerScreen
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import kotlinx.coroutines.channels.Channel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PlayerActivityDestination.argsFrom(intent)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            FindroidTheme {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    startRoute = PlayerScreenDestination,
                ) {
                    composable(PlayerScreenDestination) {
                        PlayerScreen(
                            navigator = destinationsNavigator,
                            items = args.items,
                            resultRecipient = resultRecipient(),
                        )
                    }
                }
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
