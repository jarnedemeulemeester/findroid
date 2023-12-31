package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.destinations.AddServerScreenDestination
import dev.jdtech.jellyfin.destinations.LoginScreenDestination
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

        var startRoute = NavGraphs.root.startRoute
        if (checkServersEmpty()) {
            startRoute = AddServerScreenDestination
        } else if (checkUser()) {
            startRoute = LoginScreenDestination
        }

        setContent {
            FindroidTheme {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    startRoute = startRoute,
                )
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
