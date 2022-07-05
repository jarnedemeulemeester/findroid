package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityMainAppBinding
import dev.jdtech.jellyfin.fragments.HomeFragmentDirections
import dev.jdtech.jellyfin.utils.loadDownloadLocation
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainAppBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainAppBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        setSupportActionBar(binding.mainToolbar)

        val navController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.mediaFragment, R.id.favoriteFragment, R.id.downloadFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        // navView.setupWithNavController(navController)
        // Don't save the state of other main navigation items, only this experimental function allows turning off this behavior
        NavigationUI.setupWithNavController(navView, navController, false)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility = when (destination.id) {
                R.id.twoPaneSettingsFragment, R.id.serverSelectFragment, R.id.addServerFragment, R.id.loginFragment, R.id.about_libraries_dest -> View.GONE
                else -> View.VISIBLE
            }
            if (destination.id == R.id.about_libraries_dest) binding.mainToolbar.title = getString(R.string.app_info)
        }

        loadDownloadLocation(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onNavigateToAddServer(lifecycleScope) {
                    if (it) {
                        navController.navigate(HomeFragmentDirections.actionHomeFragmentToAddServerFragment())
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}