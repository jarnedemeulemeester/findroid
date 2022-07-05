package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityMainTvBinding
import dev.jdtech.jellyfin.tv.ui.HomeFragmentDirections
import dev.jdtech.jellyfin.utils.loadDownloadLocation
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class MainActivityTv : FragmentActivity() {

    private lateinit var binding: ActivityMainTvBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainTvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.tv_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

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
}