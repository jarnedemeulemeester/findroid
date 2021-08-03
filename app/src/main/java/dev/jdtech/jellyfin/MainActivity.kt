package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityMainBinding
import dev.jdtech.jellyfin.fragments.InitializingFragmentDirections
import dev.jdtech.jellyfin.viewmodels.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)

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
                R.id.homeFragment, R.id.mediaFragment, R.id.favoriteFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility = when (destination.id) {
                R.id.settingsFragment, R.id.serverSelectFragment, R.id.addServerFragment, R.id.loginFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        viewModel.navigateToAddServer.observe(this, {
            if (it) {
                navController.navigate(InitializingFragmentDirections.actionInitializingFragmentToAddServerFragment3())
                viewModel.doneNavigateToAddServer()
            }
        })

        viewModel.doneLoading.observe(this, {
            if (it) {
                navController.navigate(InitializingFragmentDirections.actionInitializingFragmentToNavigationHome())
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}