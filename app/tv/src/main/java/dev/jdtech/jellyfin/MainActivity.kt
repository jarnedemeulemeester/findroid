package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.ui.NavGraphs
import dev.jdtech.jellyfin.ui.destinations.AddServerScreenDestination
import dev.jdtech.jellyfin.ui.destinations.DirectionDestination
import dev.jdtech.jellyfin.ui.destinations.HomeScreenDestination
import dev.jdtech.jellyfin.ui.destinations.LoginScreenDestination
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var database: ServerDatabaseDao

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var startRoute: DirectionDestination = HomeScreenDestination
        if (checkServersEmpty()) {
            startRoute = AddServerScreenDestination
        } else if (checkUser()) {
            startRoute = LoginScreenDestination
        }

        setContent {
            FindroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        startRoute = startRoute
                    )
                }
            }
        }
    }

    private fun checkServersEmpty(): Boolean {
        if (!viewModel.startDestinationChanged) {
            val nServers = database.getServersCount()
            if (nServers < 1) {
                viewModel.startDestinationChanged = true
                return true
            }
        }
        return false
    }

    private fun checkUser(): Boolean {
        if (!viewModel.startDestinationChanged) {
            appPreferences.currentServer?.let {
                val currentUser = database.getServerCurrentUser(it)
                if (currentUser == null) {
                    viewModel.startDestinationChanged = true
                    return true
                }
            }
        }
        return false
    }
}
