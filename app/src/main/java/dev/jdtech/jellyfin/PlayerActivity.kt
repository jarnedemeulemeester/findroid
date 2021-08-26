package dev.jdtech.jellyfin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
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
            viewModel.initializePlayer(args.items)
        }
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        viewModel.playWhenReady = viewModel.player.value?.playWhenReady == true
        playerView.player?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        viewModel.player.value?.playWhenReady = viewModel.playWhenReady
        hideSystemUI()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        // These methods are deprecated but we still use them because the new WindowInsetsControllerCompat has a bug which makes the action bar reappear
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}

