package dev.jdtech.jellyfin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.navArgs
import com.google.android.exoplayer2.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerActivityViewModel by viewModels()

    private val args: PlayerActivityArgs by navArgs()

    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PlayerActivity", "onCreate")
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.video_view)

        viewModel.player.observe(this, {
            playerView.player = it
        })

        if (viewModel.player.value == null) {
            viewModel.initializePlayer(args.itemId)
        }
        hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PlayerActivity", "onDestroy")
        showSystemUI()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, playerView).show(WindowInsetsCompat.Type.systemBars())
    }
}

