package dev.jdtech.jellyfin

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TrackSelectionDialogBuilder
import androidx.navigation.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.dialogs.SpeedSelectionDialogFragment
import dev.jdtech.jellyfin.dialogs.TrackSelectionDialogFragment
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.utils.PlayerGestureHelper
import dev.jdtech.jellyfin.utils.PreviewScrubListener
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import timber.log.Timber
import javax.inject.Inject
import dev.jdtech.jellyfin.player.video.R as PlayerVideoR

var isControlsLocked: Boolean = false

@AndroidEntryPoint
class PlayerActivity : BasePlayerActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    lateinit var binding: ActivityPlayerBinding
    private var playerGestureHelper: PlayerGestureHelper? = null
    override val viewModel: PlayerActivityViewModel by viewModels()

    private val isPipSupported by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Creating player activity")

        val args: PlayerActivityArgs by navArgs()

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
                getSystemService(Context.AUDIO_SERVICE) as AudioManager,
            )
        }

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }

        binding.playerView.findViewById<View>(R.id.back_button_alt).setOnClickListener {
            finish()
            isControlsLocked = false
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)

        viewModel.currentItemTitle.observe(this) { title ->
            videoNameTextView.text = title
        }

        val audioButton = binding.playerView.findViewById<ImageButton>(R.id.btn_audio_track)
        val subtitleButton = binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)
        val speedButton = binding.playerView.findViewById<ImageButton>(R.id.btn_speed)
        val skipIntroButton = binding.playerView.findViewById<Button>(R.id.btn_skip_intro)
        val pipButton = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        val lockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_lockview)
        val unlockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_unlock)

        audioButton.isEnabled = false
        audioButton.imageAlpha = 75

        lockButton.isEnabled = false
        lockButton.imageAlpha = 75

        subtitleButton.isEnabled = false
        subtitleButton.imageAlpha = 75

        speedButton.isEnabled = false
        speedButton.imageAlpha = 75

        if (isPipSupported) {
            pipButton.isEnabled = false
            pipButton.imageAlpha = 75
        } else {
            pipButton.isVisible = false
        }

        audioButton.setOnClickListener {
            when (viewModel.player) {
                is MPVPlayer -> {
                    TrackSelectionDialogFragment(TrackType.AUDIO, viewModel).show(
                        supportFragmentManager,
                        "trackselectiondialog",
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
                        this,
                        resources.getString(PlayerVideoR.string.select_audio_track),
                        viewModel.player,
                        C.TRACK_TYPE_AUDIO,
                    )
                    val trackSelectionDialog = trackSelectionDialogBuilder.build()
                    trackSelectionDialog.show()
                }
            }
        }

        val exoPlayerControlView = findViewById<FrameLayout>(R.id.player_controls)
        val lockedLayout = findViewById<FrameLayout>(R.id.locked_player_view)

        lockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.GONE
            lockedLayout.visibility = View.VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            isControlsLocked = true
        }

        unlockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.VISIBLE
            lockedLayout.visibility = View.GONE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isControlsLocked = false
        }

        subtitleButton.setOnClickListener {
            when (viewModel.player) {
                is MPVPlayer -> {
                    TrackSelectionDialogFragment(TrackType.SUBTITLE, viewModel).show(
                        supportFragmentManager,
                        "trackselectiondialog",
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
                        this,
                        resources.getString(PlayerVideoR.string.select_subtile_track),
                        viewModel.player,
                        C.TRACK_TYPE_TEXT,
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
                "speedselectiondialog",
            )
        }

        pipButton.setOnClickListener {
            pictureInPicture()
        }

        viewModel.currentIntro.observe(this) {
            if (!isInPictureInPictureMode) {
                skipIntroButton.isVisible = it != null
            }
        }

        skipIntroButton.setOnClickListener {
            viewModel.currentIntro.value?.let {
                binding.playerView.player?.seekTo((it.introEnd * 1000).toLong())
            }
        }

        if (appPreferences.playerTrickPlay) {
            val imagePreview = binding.playerView.findViewById<ImageView>(R.id.image_preview)
            val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
            val previewScrubListener = PreviewScrubListener(
                imagePreview,
                timeBar,
                viewModel.player,
                viewModel.currentTrickPlay,
            )

            timeBar.addListener(previewScrubListener)
        }

        viewModel.fileLoaded.observe(this) {
            if (it) {
                audioButton.isEnabled = true
                audioButton.imageAlpha = 255
                lockButton.isEnabled = true
                lockButton.imageAlpha = 255
                subtitleButton.isEnabled = true
                subtitleButton.imageAlpha = 255
                speedButton.isEnabled = true
                speedButton.imageAlpha = 255
                pipButton.isEnabled = true
                pipButton.imageAlpha = 255
            }
        }

        viewModel.navigateBack.observe(this) {
            if (it) {
                finish()
            }
        }

        viewModel.initializePlayer(args.items)
        hideSystemUI()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        val args: PlayerActivityArgs by navArgs()
        viewModel.initializePlayer(args.items)
    }

    override fun onUserLeaveHint() {
        if (appPreferences.playerPipGesture && viewModel.player.isPlaying && !isControlsLocked) {
            pictureInPicture()
        }
    }

    private fun pipParams(): PictureInPictureParams {
        val landscape = binding.playerView.player?.videoSize?.width!! > binding.playerView.player?.videoSize?.height!!
        val displayAspectRatio = Rational(binding.playerView.width, binding.playerView.height)

        val aspectRatio = if (appPreferences.playerPipAspectRatio) {
            if (landscape) {
                Rational(16, 9)
            } else {
                Rational(9, 16)
            }
        } else {
            binding.playerView.player?.videoSize?.let {
                Rational(
                    it.width.coerceAtMost((it.height * 2.39f).toInt()),
                    it.height.coerceAtMost((it.width * 2.39f).toInt()),
                )
            }
        }

        val sourceRectHint = if (aspectRatio!! > displayAspectRatio) {
            val space = ((binding.playerView.height - (binding.playerView.width.toFloat() / aspectRatio.toFloat())) / 2).toInt()
            Rect(
                0,
                space,
                binding.playerView.width,
                (binding.playerView.width.toFloat() / aspectRatio.toFloat()).toInt() + space,
            )
        } else {
            val space = ((binding.playerView.width - (binding.playerView.height.toFloat() / aspectRatio.toFloat())) / 2).toInt()
            Rect(
                space,
                0,
                (binding.playerView.height.toFloat() / aspectRatio.toFloat()).toInt(),
                binding.playerView.height + space,
            )
        }

        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setSourceRectHint(sourceRectHint)
            .build()
    }

    private fun pictureInPicture() {
        if (!isPipSupported) {
            return
        }
        binding.playerView.useController = false
        binding.playerView.findViewById<Button>(R.id.btn_skip_intro).isVisible = false

        if (binding.playerView.player is MPVPlayer) {
            (binding.playerView.player as MPVPlayer).updateZoomMode(false)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        enterPictureInPictureMode(pipParams())
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            binding.playerView.useController = true
        }
    }
}
