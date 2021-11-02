package dev.jdtech.jellyfin

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.navArgs
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.dialogs.SpeedSelectionDialogFragment
import dev.jdtech.jellyfin.dialogs.TrackSelectionDialogFragment
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.utils.AudioController
import dev.jdtech.jellyfin.utils.VerticalSwipeListener
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import timber.log.Timber
import kotlin.math.max

@AndroidEntryPoint
class PlayerActivity : BasePlayerActivity() {

    private lateinit var binding: ActivityPlayerBinding
    override val viewModel: PlayerActivityViewModel by viewModels()
    private val args: PlayerActivityArgs by navArgs()
    private val audioController by lazy { AudioController(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Creating player activity")

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player

        val playerControls = binding.playerView.findViewById<View>(R.id.player_controls)
        setupVolumeControl()

        configureInsets(playerControls)

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)

        viewModel.currentItemTitle.observe(this, { title ->
            videoNameTextView.text = title
        })

        val audioButton = binding.playerView.findViewById<ImageButton>(R.id.btn_audio_track)
        val subtitleButton = binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)
        val speedButton = binding.playerView.findViewById<ImageButton>(R.id.btn_speed)

        audioButton.isEnabled = false
        audioButton.imageAlpha = 75

        subtitleButton.isEnabled = false
        subtitleButton.imageAlpha = 75

        speedButton.isEnabled = false
        speedButton.imageAlpha = 75

        audioButton.setOnClickListener {
            when (viewModel.player) {
                is MPVPlayer -> {
                    TrackSelectionDialogFragment(TrackType.AUDIO, viewModel).show(
                        supportFragmentManager,
                        "trackselectiondialog"
                    )
                }
                is SimpleExoPlayer -> {
                    val mappedTrackInfo =
                        viewModel.trackSelector.currentMappedTrackInfo ?: return@setOnClickListener

                    var audioRenderer: Int? = null
                    for (i in 0 until mappedTrackInfo.rendererCount) {
                        if (isRendererType(mappedTrackInfo, i, C.TRACK_TYPE_AUDIO)) {
                            audioRenderer = i
                        }
                    }

                    if (audioRenderer == null) return@setOnClickListener

                    val trackSelectionDialogBuilder = TrackSelectionDialogBuilder(
                        this, resources.getString(R.string.select_audio_track),
                        viewModel.trackSelector, audioRenderer
                    )
                    val trackSelectionDialog = trackSelectionDialogBuilder.build()
                    trackSelectionDialog.show()
                }
            }
        }

        subtitleButton.setOnClickListener {
            when (viewModel.player) {
                is MPVPlayer -> {
                    TrackSelectionDialogFragment(TrackType.SUBTITLE, viewModel).show(
                        supportFragmentManager,
                        "trackselectiondialog"
                    )
                }
                is SimpleExoPlayer -> {
                    val mappedTrackInfo =
                        viewModel.trackSelector.currentMappedTrackInfo ?: return@setOnClickListener

                    var subtitleRenderer: Int? = null
                    for (i in 0 until mappedTrackInfo.rendererCount) {
                        if (isRendererType(mappedTrackInfo, i, C.TRACK_TYPE_TEXT)) {
                            subtitleRenderer = i
                        }
                    }

                    if (subtitleRenderer == null) return@setOnClickListener

                    val trackSelectionDialogBuilder = TrackSelectionDialogBuilder(
                        this, resources.getString(R.string.select_subtile_track),
                        viewModel.trackSelector, subtitleRenderer
                    )
                    val trackSelectionDialog = trackSelectionDialogBuilder.build()
                    trackSelectionDialog.show()
                }
            }
        }

        speedButton.setOnClickListener {
            SpeedSelectionDialogFragment(viewModel).show(
                supportFragmentManager,
                "speedselectiondialog"
            )
        }

        viewModel.fileLoaded.observe(this, {
            if (it) {
                audioButton.isEnabled = true
                audioButton.imageAlpha = 255
                subtitleButton.isEnabled = true
                subtitleButton.imageAlpha = 255
                speedButton.isEnabled = true
                speedButton.imageAlpha = 255
            }
        })

        viewModel.navigateBack.observe(this, {
            if (it) {
                onBackPressed()
            }
        })

        viewModel.initializePlayer(args.items)
        hideSystemUI()
    }

    private fun setupVolumeControl() {
        val height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            windowManager.defaultDisplay.height
        }
        binding.playerView.setOnTouchListener(VerticalSwipeListener(
            onUp = { audioController.volumeUp() },
            onDown = { audioController.volumeDown() },
            onTouch = { audioController.showVolumeSlider() },
            threshold = max(height / 8, 100)
        ))
    }
}

