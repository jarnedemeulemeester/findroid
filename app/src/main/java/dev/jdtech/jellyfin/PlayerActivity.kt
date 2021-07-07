package dev.jdtech.jellyfin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.navigation.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.MimeTypes
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

        playerView = findViewById(R.id.video_view)

        viewModel.player.observe(this, {
            playerView.player = it
        })

        if (viewModel.player.value == null) {
            viewModel.initializePlayer(args.itemId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PlayerActivity", "onDestroy")
    }
}