package dev.jdtech.jellyfin

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import dev.jdtech.jellyfin.work.MediaDownloadWorker

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    /** Route of the item to open, requested by tapping a download notification. */
    private var pendingItemRoute: Any? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not open the item again when the activity is recreated
        if (savedInstanceState == null) {
            pendingItemRoute = intent.toItemRoute()
        }

        enableEdgeToEdge()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme(dynamicColor = state.isDynamicColors) {
                val navController = rememberNavController()
                if (!state.isLoading) {
                    CompositionLocalProvider(LocalOfflineMode provides state.isOfflineMode) {
                        NavigationRoot(
                            navController = navController,
                            hasServers = state.hasServers,
                            hasCurrentServer = state.hasCurrentServer,
                            hasCurrentUser = state.hasCurrentUser,
                            pendingItemRoute = pendingItemRoute,
                            onPendingItemRouteHandled = { pendingItemRoute = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingItemRoute = intent.toItemRoute()
    }

    private fun Intent.toItemRoute(): Any? {
        if (action != MediaDownloadWorker.ACTION_VIEW_ITEM) return null
        val itemId = getStringExtra(MediaDownloadWorker.EXTRA_ITEM_ID) ?: return null
        return when (getStringExtra(MediaDownloadWorker.EXTRA_ITEM_KIND)) {
            MediaDownloadWorker.ITEM_KIND_MOVIE -> MovieRoute(movieId = itemId)
            MediaDownloadWorker.ITEM_KIND_EPISODE -> EpisodeRoute(episodeId = itemId)
            else -> null
        }
    }
}
