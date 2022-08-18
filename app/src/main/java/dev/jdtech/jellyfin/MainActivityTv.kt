package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.databinding.ActivityMainTvBinding
import dev.jdtech.jellyfin.utils.loadDownloadLocation
import javax.inject.Inject

@AndroidEntryPoint
internal class MainActivityTv : FragmentActivity() {

    private lateinit var binding: ActivityMainTvBinding
    @Inject
    lateinit var database: ServerDatabaseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainTvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.tv_nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        val nServers = database.getServersCount()
        if (nServers < 1) {
            val inflater = navController.navInflater
            val graph = inflater.inflate(R.navigation.tv_navigation)
            graph.setStartDestination(R.id.addServerTvFragment)
            navController.setGraph(graph, intent.extras)
        }

        loadDownloadLocation(applicationContext)
    }
}