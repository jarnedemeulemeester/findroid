package dev.jdtech.jellyfin

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.navArgs
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.dialogs.SpeedSelectionDialogFragment
import dev.jdtech.jellyfin.dialogs.TrackSelectionDialogFragment
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.utils.AppPreferences
import dev.jdtech.jellyfin.utils.PlayerGestureHelper
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : BasePlayerActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    lateinit var binding: ActivityPlayerBinding
    private var playerGestureHelper: PlayerGestureHelper? = null
    override val viewModel: PlayerActivityViewModel by viewModels()
    private val args: PlayerActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Creating player activity")

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player

        val playerControls = binding.playerView.findViewById<View>(R.id.player_controls)

        configureInsets(playerControls)

        if (appPreferences.playerGestures) {
            playerGestureHelper = PlayerGestureHelper(
                appPreferences,
                this,
                binding.playerView,
                getSystemService(Context.AUDIO_SERVICE) as AudioManager
            )
        }

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)

        viewModel.currentItemTitle.observe(this, { title ->
            videoNameTextView.text = title
        })

        val audioButton = binding.playerView.findViewById<ImageButton>(R.id.exo_audio_track)
        val subtitleButton = binding.playerView.findViewById<ImageButton>(R.id.exo_subtitle)
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
                is ExoPlayer -> {
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
                is ExoPlayer -> {
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
                    trackSelectionDialogBuilder.setShowDisableOption(true)

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
}

