package dev.jdtech.jellyfin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.navArgs
import com.google.android.exoplayer2.ui.StyledPlayerView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import timber.log.Timber

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerActivityViewModel by viewModels()

    private val args: PlayerActivityArgs by navArgs()

    private lateinit var playerView: StyledPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Creating player activity")
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.video_view)

        viewModel.player.observe(this, {
            playerView.player = it
        })

        viewModel.navigateBack.observe(this, {
            if (it) {
                onBackPressed()
            }
        })

        if (viewModel.player.value == null) {
            viewModel.initializePlayer(args.items, args.playbackPosition)
        }
        hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Destroying player activity")
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

