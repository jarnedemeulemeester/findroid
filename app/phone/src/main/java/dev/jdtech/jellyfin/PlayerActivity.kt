package dev.jdtech.jellyfin

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import androidx.navigation.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.dialogs.SpeedSelectionDialogFragment
import dev.jdtech.jellyfin.dialogs.TrackSelectionDialogFragment
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.utils.PlayerGestureHelper
import dev.jdtech.jellyfin.utils.PreviewScrubListener
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private var previewScrubListener: PreviewScrubListener? = null
    private var sleepJob: Job? = null
    private var wasDialogShown : Boolean = false
    private var isSleepModeEnabled: Boolean = false

    private val isPipSupported by lazy {
        // Check if device has PiP feature
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return@lazy false
        }

        // Check if PiP is enabled for the app
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args: PlayerActivityArgs by navArgs()

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player
        binding.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (visibility == View.GONE) {
                    hideSystemUI()
                }
            },
        )

        val playerControls = binding.playerView.findViewById<View>(R.id.player_controls)
        val lockedControls = binding.playerView.findViewById<View>(R.id.locked_player_view)

        isControlsLocked = false

        configureInsets(playerControls)
        configureInsets(lockedControls)

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
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)

        val audioButton = binding.playerView.findViewById<ImageButton>(R.id.btn_audio_track)
        val subtitleButton = binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)
        val speedButton = binding.playerView.findViewById<ImageButton>(R.id.btn_speed)
        val skipIntroButton = binding.playerView.findViewById<Button>(R.id.btn_skip_intro)
        val pipButton = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        val lockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_lockview)
        val sleepModeButton = binding.playerView.findViewById<ImageButton>(R.id.btn_sleep_mode)
        val unlockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_unlock)
        val sleepModeUnlockButton = binding.playerView.findViewById<ImageButton>(R.id.sleep_mode_unlock)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        uiState.apply {
                            // Title
                            videoNameTextView.text = currentItemTitle

                            // Skip Intro button
                            skipIntroButton.isVisible = !isInPictureInPictureMode && currentIntro != null
                            skipIntroButton.setOnClickListener {
                                currentIntro?.let {
                                    binding.playerView.player?.seekTo((it.introEnd * 1000).toLong())
                                }
                            }

                            // Trick Play
                            previewScrubListener?.let {
                                it.currentTrickPlay = currentTrickPlay
                            }

                            // File Loaded
                            if (fileLoaded) {
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
                                if(appPreferences.sleepMode) {
                                    sleepModeButton.isEnabled = true
                                    sleepModeButton.imageAlpha = 255
                                } else {
                                    sleepModeButton.isEnabled = false
                                    sleepModeButton.imageAlpha = 0
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.navigateBack.collect {
                        if (it) finish()
                    }
                }
            }
        }

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
            val pipSpace = binding.playerView.findViewById<Space>(R.id.space_pip)
            pipButton.isVisible = false
            pipSpace.isVisible = false
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

        sleepModeButton.setOnClickListener {
            if (isSleepModeEnabled) {
                // Check if the sleep timer is currently running
                if (sleepJob?.isActive == true) {
                    sleepJob?.cancel()
                    Toast.makeText(applicationContext, "Sleep Mode timer has been disabled!", Toast.LENGTH_SHORT).show()
                }
                isSleepModeEnabled = false
            } else {
                isSleepModeEnabled = false
                wasDialogShown = true
                showSleepTimerDurationDialog()
            }
        }


        sleepModeUnlockButton.setOnClickListener{
            disableSleepMode()
            isSleepModeEnabled = false
        }

        if (appPreferences.playerTrickPlay) {
            val imagePreview = binding.playerView.findViewById<ImageView>(R.id.image_preview)
            val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
            previewScrubListener = PreviewScrubListener(
                imagePreview,
                timeBar,
                viewModel.player,
            )

            timeBar.addListener(previewScrubListener!!)
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
        val displayAspectRatio = Rational(binding.playerView.width, binding.playerView.height)

        val aspectRatio = binding.playerView.player?.videoSize?.let {
            Rational(
                it.width.coerceAtMost((it.height * 2.39f).toInt()),
                it.height.coerceAtMost((it.width * 2.39f).toInt()),
            )
        }

        val sourceRectHint = if (displayAspectRatio < aspectRatio!!) {
            val space = ((binding.playerView.height - (binding.playerView.width.toFloat() / aspectRatio.toFloat())) / 2).toInt()
            Rect(
                0,
                space,
                binding.playerView.width,
                (binding.playerView.width.toFloat() / aspectRatio.toFloat()).toInt() + space,
            )
        } else {
            val space = ((binding.playerView.width - (binding.playerView.height.toFloat() * aspectRatio.toFloat())) / 2).toInt()
            Rect(
                space,
                0,
                (binding.playerView.height.toFloat() * aspectRatio.toFloat()).toInt() + space,
                binding.playerView.height,
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

        try {
            enterPictureInPictureMode(pipParams())
        } catch (_: IllegalArgumentException) { }
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

    private fun startSleepTimer(timerDuration: Long) {
        sleepJob?.cancel() // Cancel any existing timer job
        sleepJob = CoroutineScope(Dispatchers.Main).launch {
            delay(timerDuration)
            binding.playerView.player?.playWhenReady = !binding.playerView.player?.playWhenReady!!
            binding.playerView.findViewById<FrameLayout>(R.id.player_controls).visibility = View.GONE
            binding.playerView.findViewById<FrameLayout>(R.id.sleep_mode_view).visibility = View.VISIBLE
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isControlsLocked = true
        }
        Toast.makeText(applicationContext, formatTimerMessage(timerDuration), Toast.LENGTH_SHORT).show()
    }

    private fun showSleepTimerDurationDialog() {
        val timerTexts =
            listOf("1 minute", "5 minutes", "10 minutes", "30 minutes", "1 hour", "2 hours")
        val durationValues = longArrayOf(60000L, 300000L, 600000L, 1800000L, 3600000L, 7200000L)
        var selectedDuration = durationValues[2] // Default: 10 minutes in milliseconds

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Select Sleep Timer Duration")
            .setSingleChoiceItems(
                timerTexts.toTypedArray(),
                timerTexts.indexOf("${selectedDuration / 60000} minutes")
            ) { _, which ->
                selectedDuration = durationValues[which]
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                wasDialogShown = false
            }
            .setPositiveButton("OK") { dialog, _ ->
                // Start the sleep timer with the selected duration
                startSleepTimer(selectedDuration)
                isSleepModeEnabled = true
                dialog.dismiss()
            }
            .setCancelable(true)
        wasDialogShown = true
        builder.create().show()
    }

    private fun disableSleepMode () {
        binding.playerView.player?.playWhenReady = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.playerView.findViewById<FrameLayout>(R.id.sleep_mode_view).visibility = View.GONE
        binding.playerView.findViewById<FrameLayout>(R.id.player_controls).visibility = View.VISIBLE
        isControlsLocked = false
        isSleepModeEnabled = false
    }

    private fun formatTimerMessage(timerDuration: Long): String {
        val hours = timerDuration / 3600000L
        val minutes = (timerDuration % 3600000L) / 60000L

        val hourText = if (hours == 1L) "hour" else "hours"
        val minuteText = if (minutes == 1L) "minute" else "minutes"

        return when {
            hours > 0 && minutes > 0 ->
                "Sleep Mode will be enabled in $hours $hourText and $minutes $minuteText"
            hours > 0 ->
                "Sleep Mode will be enabled in $hours $hourText"
            else ->
                "Sleep Mode will be enabled in $minutes $minuteText"
        }
    }
}
