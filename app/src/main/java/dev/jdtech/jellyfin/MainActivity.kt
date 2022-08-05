package dev.jdtech.jellyfin

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.databinding.ActivityMainBinding
import dev.jdtech.jellyfin.utils.loadDownloadLocation
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var database: ServerDatabaseDao

    @OptIn(NavigationUiSaveStateControl::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            val nServers = database.getServersCount()
            if (nServers < 1) {
                val inflater = navController.navInflater
                val graph = inflater.inflate(R.navigation.tv_navigation)
                graph.setStartDestination(R.id.addServerTvFragment)
                navController.setGraph(graph, intent.extras)
            }
        } else {
            val navView: NavigationBarView = binding.navView as NavigationBarView

            setSupportActionBar(binding.mainToolbar)


            val nServers = database.getServersCount()
            if (nServers < 1) {
                val inflater = navController.navInflater
                val graph = inflater.inflate(R.navigation.app_navigation)
                graph.setStartDestination(R.id.addServerFragment)
                navController.setGraph(graph, intent.extras)
            }

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
                binding.navView!!.visibility = when (destination.id) {
                    R.id.twoPaneSettingsFragment, R.id.serverSelectFragment, R.id.addServerFragment, R.id.loginFragment, R.id.about_libraries_dest -> View.GONE
                    else -> View.VISIBLE
                }
                if (destination.id == R.id.about_libraries_dest) binding.mainToolbar?.title =
                    getString(R.string.app_info)
            }
        }

        loadDownloadLocation(applicationContext)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}