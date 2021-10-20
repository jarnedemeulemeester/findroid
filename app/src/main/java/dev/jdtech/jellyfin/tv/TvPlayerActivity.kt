package dev.jdtech.jellyfin.tv

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.BasePlayerActivity
import dev.jdtech.jellyfin.PlayerActivityArgs
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.ActivityPlayerTvBinding
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import timber.log.Timber

@AndroidEntryPoint
internal class TvPlayerActivity : BasePlayerActivity() {

    private lateinit var binding: ActivityPlayerTvBinding
    override val viewModel: PlayerActivityViewModel by viewModels()
    private val args: PlayerActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Player activity created.")
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerTvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player
        val playerControls = binding.playerView.findViewById<View>(R.id.tv_player_controls)
        configureInsets(playerControls)

        bind()
        viewModel.initializePlayer(args.items)
        hideSystemUI()
    }

    private fun bind() = with(binding.playerView) {
        val videoNameTextView = findViewById<TextView>(R.id.video_name)
        viewModel.currentItemTitle.observe(this@TvPlayerActivity, { title ->
            videoNameTextView.text = title
        })

        findViewById<ImageButton>(R.id.exo_play_pause).apply {
            setOnClickListener {
                when {
                    viewModel.player.isPlaying -> {
                        viewModel.player.pause()
                        setImageDrawable(resources.getDrawable(R.drawable.ic_play))
                    }
                    viewModel.player.isLoading -> Unit
                    else -> {
                        viewModel.player.play()
                        setImageDrawable(resources.getDrawable(R.drawable.ic_pause))
                    }
                }
            }
        }

        findViewById<ImageButton>(R.id.btn_audio_track).apply {
            isEnabled = false
            imageAlpha = 75
        }

        findViewById<ImageButton>(R.id.btn_subtitle).apply {
            isEnabled = false
            imageAlpha = 75
        }

        findViewById<View>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }
    }
}