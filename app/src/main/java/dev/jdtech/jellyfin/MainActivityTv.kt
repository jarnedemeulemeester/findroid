package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityMainTvBinding
import dev.jdtech.jellyfin.tv.ui.HomeFragmentDirections
import dev.jdtech.jellyfin.viewmodels.MainViewModel

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

        viewModel.navigateToAddServer.observe(this, {
            if (it) {
                navController.navigate(HomeFragmentDirections.actionHomeFragmentToAddServerFragment())
                viewModel.doneNavigateToAddServer()
            }
        })
    }
}