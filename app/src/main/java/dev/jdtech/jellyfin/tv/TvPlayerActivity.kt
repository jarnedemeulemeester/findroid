package dev.jdtech.jellyfin.tv

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        bindSubtitleControl()
    }

    private fun bindSubtitleControl() {
        val subtitleBtn = binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)

        subtitleBtn.setOnClickListener {
            val items = viewModel.currentSubtitleTracks.map { track ->
                Track(track.title, track.lang, track.codec, track.selected)
            }
            subtitleBtn.showPopupWindowAbove(items)
        }
    }

    private fun View.showPopupWindowAbove(items: List<Track>) {
        val popup = PopupWindow(this.context)
        popup.contentView = LayoutInflater.from(context).inflate(R.layout.track_selector, null)
        val recyclerView = popup.contentView.findViewById<RecyclerView>(R.id.track_selector)
        recyclerView.adapter = TrackSelectorAdapter(items)

        val startViewCoords = IntArray(2)
        getLocationInWindow(startViewCoords)

        val itemHeight = resources.getDimension(R.dimen.track_selection_item_height).toInt()
        val totalHeight = items.size * itemHeight

        popup.showAsDropDown(
            binding.root,
            startViewCoords[0],
            startViewCoords[1] - totalHeight,
            Gravity.TOP
        )
    }

    data class Track(
        val title: String,
        val language: String,
        val codec: String,
        val selected: Boolean
    )

    class TrackSelectorAdapter(
        private val items: List<Track>
    ) : RecyclerView.Adapter<TrackSelectorAdapter.TrackSelectorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackSelectorViewHolder {
            return TrackSelectorViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: TrackSelectorViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class TrackSelectorViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

            fun bind(item: Track) {
                view.findViewById<TextView>(R.id.track_name).text = String.format(
                    view.resources.getString(R.string.track_selection),
                    item.language,
                    item.title,
                    item.codec
                )
                if (item.selected) view.requestFocus()
            }
        }
    }
}