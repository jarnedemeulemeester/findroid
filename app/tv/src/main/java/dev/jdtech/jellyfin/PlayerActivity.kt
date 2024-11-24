package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.ActivityDestination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.PlayerActivityDestination
import com.ramcosta.composedestinations.generated.destinations.PlayerScreenDestination
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

data class PlayerActivityNavArgs(
    val items: ArrayList<PlayerItem>,
)

@AndroidEntryPoint
@ActivityDestination<RootGraph>(
    navArgs = PlayerActivityNavArgs::class,
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
                    start = PlayerScreenDestination(args.items),
                )
            }
        }
    }
}
