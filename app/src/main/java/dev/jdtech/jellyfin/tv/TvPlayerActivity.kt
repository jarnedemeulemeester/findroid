package dev.jdtech.jellyfin.tv

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.BasePlayerActivity
import dev.jdtech.jellyfin.PlayerActivityArgs
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.ActivityPlayerTvBinding
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.tv.ui.TrackSelectorAdapter
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (!binding.playerView.isControllerVisible) {
            binding.playerView.showController()
            true
        } else {
            false
        }
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

        findViewById<View>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }

        bindAudioControl()
        bindSubtitleControl()
    }

    private fun bindAudioControl() {
        val audioBtn = binding.playerView.findViewById<ImageButton>(R.id.btn_audio_track)

        audioBtn.setOnClickListener {
            val items = viewModel.currentSubtitleTracks.toUiTrack()
            audioBtn.showPopupWindowAbove(items, TrackType.SUBTITLE)
        }
    }

    private fun bindSubtitleControl() {
        val subtitleBtn = binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)

        subtitleBtn.setOnClickListener {
            val items = viewModel.currentSubtitleTracks.toUiTrack()
            subtitleBtn.showPopupWindowAbove(items, TrackType.SUBTITLE)
        }
    }

    private fun List<MPVPlayer.Companion.Track>.toUiTrack() = map { track ->
        TrackSelectorAdapter.Track(
            title = track.title,
            language = track.lang,
            codec = track.codec,
            selected = track.selected,
            playerTrack = track
        )
    }

    private fun View.showPopupWindowAbove(items: List<TrackSelectorAdapter.Track>, type: String) {
        val popup = PopupWindow(this.context)
        popup.contentView = LayoutInflater.from(context).inflate(R.layout.track_selector, null)
        val recyclerView = popup.contentView.findViewById<RecyclerView>(R.id.track_selector)

        recyclerView.adapter = TrackSelectorAdapter(items, viewModel, type) { popup.dismiss() }

        val startViewCoords = IntArray(2)
        getLocationInWindow(startViewCoords)

        val itemHeight = resources.getDimension(R.dimen.track_selection_item_height).toInt()
        val totalHeight = items.size * itemHeight

        popup.showAsDropDown(
            binding.root,
            startViewCoords.first(),
            startViewCoords.last() - totalHeight,
            Gravity.TOP
        )
    }
}