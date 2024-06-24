package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

data class PlayerActivityNavArgs(
    val items: ArrayList<PlayerItem>,
)

@AndroidEntryPoint
@ActivityDestination(
    navArgsDelegate = PlayerActivityNavArgs::class,
)
class PlayerActivity : ComponentActivity() {
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
}
