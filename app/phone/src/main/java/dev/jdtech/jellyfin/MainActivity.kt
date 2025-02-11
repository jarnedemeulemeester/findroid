package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import dev.jdtech.jellyfin.work.SyncWorker

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme(
                darkTheme = state.isDarkTheme
            ) {
                val navController = rememberNavController()
                if (!state.isLoading) {
                    NavigationRoot(
                        navController = navController,
                        hasServers = state.hasServers,
                        hasCurrentServer = state.hasCurrentServer,
                        hasCurrentUser = state.hasCurrentUser,
                    )
                }
            }
        }

        scheduleUserDataSync()
    }

    private fun scheduleUserDataSync() {
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED,
                    )
                    .build(),
            )
            .build()

        val workManager = WorkManager.getInstance(applicationContext)

        workManager.beginUniqueWork("syncUserData", ExistingWorkPolicy.KEEP, syncWorkRequest)
            .enqueue()
    }
}
