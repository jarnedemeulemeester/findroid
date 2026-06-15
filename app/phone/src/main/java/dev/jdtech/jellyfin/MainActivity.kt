package dev.jdtech.jellyfin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var jellyfinRepository: JellyfinRepository

    private var pendingDeepLinkItemId by mutableStateOf<UUID?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        pendingDeepLinkItemId = parseDeepLink(intent)

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
                        )

                        val itemId = pendingDeepLinkItemId
                        if (itemId != null) {
                            LaunchedEffect(itemId, state.hasCurrentUser) {
                                if (state.hasCurrentUser) {
                                    val item = runCatching { jellyfinRepository.getItem(itemId) }
                                        .onFailure { Timber.w(it, "Deep link lookup failed for %s", itemId) }
                                        .getOrNull()
                                    if (item != null) {
                                        navigateToItem(navController, item)
                                    }
                                }
                                pendingDeepLinkItemId = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent)?.let { pendingDeepLinkItemId = it }
    }

    private fun parseDeepLink(intent: Intent?): UUID? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val data: Uri = intent.data ?: return null
        if (data.scheme != "jellyfin" || data.host != "item") return null
        val rawId = data.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { UUID.fromString(rawId) }
            .onFailure { Timber.w("Ignoring deep link with invalid item id: %s", rawId) }
            .getOrNull()
    }
}
