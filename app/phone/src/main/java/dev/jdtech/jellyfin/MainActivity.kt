package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.databinding.ActivityMainBinding
import dev.jdtech.jellyfin.utils.loadDownloadLocation
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()
    private var castSession: CastSession? = null
    private lateinit var sessionManager: SessionManager
    private val sessionManagerListener: SessionManagerListener<CastSession> =
        SessionManagerListenerImpl(this)

    @Inject
    lateinit var database: ServerDatabaseDao

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var navController: NavController

    @Inject
    lateinit var jellyfinApi: JellyfinApi

    @OptIn(NavigationUiSaveStateControl::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (appPreferences.amoledTheme) {
            setTheme(R.style.Theme_FindroidAMOLED)
        }

        sessionManager = CastContext.getSharedInstance(this).sessionManager
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.app_navigation)

        checkServersEmpty(graph) {
            navController.setGraph(graph, intent.extras)
        }
        checkUser(graph) {
            navController.setGraph(graph, intent.extras)
        }

        val navView: NavigationBarView = binding.navView as NavigationBarView

        setSupportActionBar(binding.mainToolbar)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.mediaFragment,
                R.id.favoriteFragment,
                R.id.downloadFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        // navView.setupWithNavController(navController)
        // Don't save the state of other main navigation items, only this experimental function allows turning off this behavior
        NavigationUI.setupWithNavController(navView, navController, false)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility = when (destination.id) {
                R.id.twoPaneSettingsFragment, R.id.serverSelectFragment, R.id.addServerFragment, R.id.loginFragment, R.id.about_libraries_dest, R.id.usersFragment, R.id.serverAddressesFragment -> View.GONE
                else -> View.VISIBLE
            }
            if (destination.id == R.id.about_libraries_dest) binding.mainToolbar.title =
                getString(R.string.app_info)
        }

        loadDownloadLocation(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        castSession = sessionManager.currentCastSession
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    override fun onPause() {
        super.onPause()
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        castSession = null
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    private fun checkServersEmpty(graph: NavGraph, onServersEmpty: () -> Unit = {}) {
        if (!viewModel.startDestinationChanged) {
            val nServers = database.getServersCount()
            if (nServers < 1) {
                graph.setStartDestination(R.id.addServerFragment)
                viewModel.startDestinationChanged = true
                onServersEmpty()
            }
        }
    }

    private fun checkUser(graph: NavGraph, onNoUser: () -> Unit = {}) {
        if (!viewModel.startDestinationChanged) {
            appPreferences.currentServer?.let {
                val currentUser = database.getServerCurrentUser(it)
                if (currentUser == null) {
                    graph.setStartDestination(R.id.loginFragment)
                    viewModel.startDestinationChanged = true
                    onNoUser()
                }
            }
        }
    }

    companion object {
        private class SessionManagerListenerImpl(private val mainActivity: MainActivity) :
            SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) {
                mainActivity.invalidateOptionsMenu()
                val thing =
                    "{\"options\":{},\"command\":\"Identify\",\"userId\":\"${mainActivity.jellyfinApi.userId}\",\"deviceId\":\"${mainActivity.jellyfinApi.api.deviceInfo.id}\",\"accessToken\":\"${mainActivity.jellyfinApi.api.accessToken}\",\"serverAddress\":\"${mainActivity.jellyfinApi.api.baseUrl}\",\"serverId\":\"\",\"serverVersion\":\"\",\"receiverName\":\"\"}"
                session.sendMessage("urn:x-cast:com.connectsdk", thing)
                session.setMessageReceivedCallbacks(
                    "urn:x-cast:com.connectsdk"
                ) { _, _, message -> Timber.i(message) }
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                mainActivity.invalidateOptionsMenu()
            }

            override fun onSessionEnded(session: CastSession, error: Int) {
                //            finish()
            }

            override fun onSessionEnding(p0: CastSession) {
            }

            override fun onSessionResumeFailed(p0: CastSession, p1: Int) {
            }

            override fun onSessionResuming(p0: CastSession, p1: String) {
            }

            override fun onSessionStartFailed(p0: CastSession, p1: Int) {
            }

            override fun onSessionStarting(p0: CastSession) {
            }

            override fun onSessionSuspended(p0: CastSession, p1: Int) {
            }
        }
    }
}
