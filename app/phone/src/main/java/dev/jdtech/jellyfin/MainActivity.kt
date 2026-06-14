package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.offline.OfflineDownloadRecovery
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var didResumeInterruptedDownloads = false

    @Inject lateinit var offlineDownloadRecovery: OfflineDownloadRecovery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (didResumeInterruptedDownloads) return
        didResumeInterruptedDownloads = true
        lifecycleScope.launch {
            runCatching {
                    val markedCount = offlineDownloadRecovery.markInterruptedDownloads()
                    val cleanedCount = offlineDownloadRecovery.cleanupCanceledDownloads()
                    val resumedCount = offlineDownloadRecovery.resumeInterruptedDownloads()
                    Timber.i(
                        "Foreground offline recovery finished: marked=%d cleaned=%d resumed=%d",
                        markedCount,
                        cleanedCount,
                        resumedCount,
                    )
                }
                .onFailure { error -> Timber.e(error, "Foreground offline recovery failed") }
        }
    }
}
