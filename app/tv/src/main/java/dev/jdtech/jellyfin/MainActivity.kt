package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: ServerDatabaseDao

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FindroidTheme {
                val navController = rememberNavController()
                NavigationRoot(
                    navController = navController,
                    hasServers = checkHasServers(),
                    hasCurrentServer = checkHasCurrentServer(),
                    hasCurrentUser = checkHasCurrentUser(),
                )
            }
        }
    }

    private fun checkHasServers(): Boolean {
        val nServers = database.getServersCount()
        return nServers > 0
    }

    private fun checkHasCurrentServer(): Boolean {
        return appPreferences.currentServer?.let {
            database.get(it) != null
        } == true
    }

    private fun checkHasCurrentUser(): Boolean {
        return appPreferences.currentServer?.let {
            database.getServerCurrentUser(it) != null
        } == true
    }
}
