package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import dev.jdtech.jellyfin.work.scheduleUserDataSync
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Leaving offline mode restarts this activity; sync offline progress to the server.
        if (savedInstanceState == null && !appPreferences.getValue(appPreferences.offlineMode)) {
            scheduleUserDataSync(this, restart = true)
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme(dynamicColor = state.isDynamicColors) {
                if (!state.isLoading) {
                    CompositionLocalProvider(LocalOfflineMode provides state.isOfflineMode) {
                        NavigationRoot(
                            hasServers = state.hasServers,
                            hasCurrentServer = state.hasCurrentServer,
                            hasCurrentUser = state.hasCurrentUser,
                        )
                    }
                }
            }
        }
    }
}
