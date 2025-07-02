package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.viewmodels.MainViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme {
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
    }
}
