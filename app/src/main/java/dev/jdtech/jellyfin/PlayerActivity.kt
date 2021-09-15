package dev.jdtech.jellyfin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import timber.log.Timber

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerActivityViewModel by viewModels()
    private val args: PlayerActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Creating player activity")
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)

        viewModel.currentItemTitle.observe(this, { title ->
            videoNameTextView.text = title
        })

        viewModel.navigateBack.observe(this, {
            if (it) {
                onBackPressed()
            }
        })

        viewModel.initializePlayer(args.items)
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        viewModel.playWhenReady = viewModel.player.playWhenReady == true
        viewModel.player.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        viewModel.player.playWhenReady = viewModel.playWhenReady
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

