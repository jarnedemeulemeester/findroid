package dev.jdtech.jellyfin.player.xr

import android.os.Bundle
import android.view.Surface as AndroidSurface
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.SurfaceEntity
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.player.local.domain.getTrackNames
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.player.local.R as LocalR

@AndroidEntryPoint
class XrPlayerActivity : AppCompatActivity() {

    companion object {
        /**
         * Whether to use the XR SurfaceEntity for stereoscopic 3D video rendering.
         *
         * When true: video is sent to SurfaceEntity.getSurface() via a plain ExoPlayer,
         * and the SurfaceEntity handles stereo eye separation natively (per the Android XR
         * spatial video API). The PlayerView is hidden in this mode.
         *
         * When false: video plays in a standard PlayerView with left-eye cropping for
         * SBS/TB content (2D fallback).
         *
         * Currently disabled because the SurfaceEntity media rendering pipeline requires
         * RenderEyeTarget support, which is not available on API 34 (Samsung Galaxy XR
         * launch firmware). Set to true once a firmware update enables this.
         *
         * See: https://developer.android.com/develop/xr/jetpack-xr-sdk/add-spatial-video
         */
        private const val USE_SURFACE_ENTITY_RENDERING = false
    }

    private val viewModel: PlayerViewModel by viewModels()

    private var xrSession: Session? = null
    private var surfaceEntity: SurfaceEntity? = null
    private var mediaSession: MediaSession? = null
    private var videoSurface: AndroidSurface? = null
    private var currentStereoMode: String = "mono"
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = UUID.fromString(intent.extras!!.getString("itemId"))
        val itemKind = intent.extras!!.getString("itemKind") ?: ""
        val startFromBeginning = intent.extras!!.getBoolean("startFromBeginning")
        val stereoModeStr = intent.extras?.getString("stereoMode") ?: "mono"
        currentStereoMode = stereoModeStr

        // Initialize XR Session for spatial features (non-fatal if unavailable)
        try {
            val result = Session.create(this)
            if (result is SessionCreateSuccess) {
                xrSession = result.session
                createSurfaceEntity()
            } else {
                Timber.w("XR session creation failed: $result, using 2D playback")
            }
        } catch (e: Exception) {
            Timber.w(e, "XR session not available, using 2D playback")
        }

        // SurfaceEntity rendering path: use a plain ExoPlayer connected to the
        // SurfaceEntity surface for native stereoscopic 3D playback.
        if (USE_SURFACE_ENTITY_RENDERING && isStereo3d()) {
            val xrPlayer = ExoPlayer.Builder(this).build()
            viewModel.replacePlayer(xrPlayer)
            if (videoSurface != null) {
                xrPlayer.setVideoSurface(videoSurface)
                Timber.d("Connected XR player to SurfaceEntity surface for 3D playback")
            }
        }

        // Listen for video size changes to update the spatial surface shape
        viewModel.player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    updateSurfaceShape(videoSize)
                }
            }
        })

        setContent {
            XrPlayerTheme {
                XrPlayerScreen(
                    viewModel = viewModel,
                    initialStereoMode = stereoModeStr,
                    useSurfaceEntityRendering = USE_SURFACE_ENTITY_RENDERING,
                    onStereoModeChange = { newMode ->
                        currentStereoMode = newMode

                        if (USE_SURFACE_ENTITY_RENDERING) {
                            recreateSurfaceEntity()
                            if (isStereo3d() && videoSurface != null) {
                                (viewModel.player as? ExoPlayer)?.setVideoSurface(videoSurface)
                            }
                        } else {
                            applyStereoMode()
                            recreateSurfaceEntity()
                        }
                    },
                    onPlayerViewCreated = { pv ->
                        playerView = pv
                        if (!USE_SURFACE_ENTITY_RENDERING) {
                            applyStereoMode()
                        }
                    },
                    onBackClick = {
                        finish()
                    },
                )
            }
        }

        viewModel.initializePlayer(
            itemId = itemId,
            itemKind = itemKind,
            startFromBeginning = startFromBeginning,
        )
    }

    private fun isStereo3d(): Boolean = currentStereoMode != "mono"

    /**
     * 2D fallback: crop the PlayerView's SurfaceView to show only one eye.
     * For SBS: scale 2x horizontally from the left edge (left eye).
     * For TB: scale 2x vertically from the top edge (top eye).
     */
    private fun applyStereoMode() {
        val videoSurfaceView = playerView?.videoSurfaceView ?: return
        when (currentStereoMode) {
            "sbs" -> {
                videoSurfaceView.scaleX = 2f
                videoSurfaceView.pivotX = 0f
                videoSurfaceView.scaleY = 1f
                videoSurfaceView.pivotY = 0f
            }
            "top_bottom" -> {
                videoSurfaceView.scaleX = 1f
                videoSurfaceView.pivotX = 0f
                videoSurfaceView.scaleY = 2f
                videoSurfaceView.pivotY = 0f
            }
            else -> {
                videoSurfaceView.scaleX = 1f
                videoSurfaceView.scaleY = 1f
                videoSurfaceView.pivotX = videoSurfaceView.width / 2f
                videoSurfaceView.pivotY = videoSurfaceView.height / 2f
            }
        }
    }

    private fun createSurfaceEntity() {
        val session = xrSession ?: return
        val xrStereoMode = mapStereoMode(currentStereoMode)
        val pose = Pose(Vector3(0f, 0f, -2.0f))
        val shape = SurfaceEntity.Shape.Quad(FloatSize2d(2.0f, 1.125f))

        try {
            surfaceEntity = if (xrStereoMode != null) {
                SurfaceEntity.create(session = session, pose = pose, shape = shape, stereoMode = xrStereoMode)
            } else {
                SurfaceEntity.create(session = session, pose = pose, shape = shape)
            }
            if (USE_SURFACE_ENTITY_RENDERING) {
                videoSurface = surfaceEntity?.getSurface()
            }
            Timber.d("Created SurfaceEntity with stereoMode=$currentStereoMode, shape=$shape")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create SurfaceEntity")
        }
    }

    private fun recreateSurfaceEntity() {
        if (USE_SURFACE_ENTITY_RENDERING) {
            (viewModel.player as? ExoPlayer)?.clearVideoSurface()
        }
        surfaceEntity?.dispose()
        surfaceEntity = null
        videoSurface = null
        createSurfaceEntity()
        val videoSize = viewModel.player.videoSize
        if (videoSize.width > 0 && videoSize.height > 0) {
            updateSurfaceShape(videoSize)
        }
    }

    private fun updateSurfaceShape(videoSize: VideoSize) {
        val entity = surfaceEntity ?: return
        val displayWidth = when (currentStereoMode) {
            "sbs" -> videoSize.width / 2f
            else -> videoSize.width.toFloat()
        }
        val displayHeight = when (currentStereoMode) {
            "top_bottom" -> videoSize.height / 2f
            else -> videoSize.height.toFloat()
        }
        val aspectRatio = displayWidth / displayHeight
        val quadWidth = 2.0f
        val quadHeight = quadWidth / aspectRatio
        try {
            entity.shape = SurfaceEntity.Shape.Quad(FloatSize2d(quadWidth, quadHeight))
            Timber.d("Updated XR surface shape: ${displayWidth}x${displayHeight}, aspect=$aspectRatio, quad=${quadWidth}x${quadHeight}m")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update surface shape")
        }
    }

    private fun mapStereoMode(mode: String): SurfaceEntity.StereoMode? {
        return when (mode) {
            "sbs" -> SurfaceEntity.StereoMode.SIDE_BY_SIDE
            "top_bottom" -> SurfaceEntity.StereoMode.TOP_BOTTOM
            else -> null
        }
    }

    override fun onStart() {
        super.onStart()
        mediaSession = MediaSession.Builder(this, viewModel.player).build()
    }

    override fun onResume() {
        super.onResume()
        viewModel.player.playWhenReady = viewModel.playWhenReady
        if (USE_SURFACE_ENTITY_RENDERING && isStereo3d() && videoSurface != null) {
            (viewModel.player as? ExoPlayer)?.setVideoSurface(videoSurface)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.playWhenReady = viewModel.player.playWhenReady
        viewModel.player.playWhenReady = false
        viewModel.updatePlaybackProgress()
    }

    override fun onStop() {
        super.onStop()
        mediaSession?.release()
        mediaSession = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (USE_SURFACE_ENTITY_RENDERING) {
            (viewModel.player as? ExoPlayer)?.clearVideoSurface()
        }
        surfaceEntity?.dispose()
        surfaceEntity = null
        videoSurface = null
        playerView = null
    }
}

// region Compose UI

@Composable
private fun XrPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}

@Composable
private fun XrPlayerScreen(
    viewModel: PlayerViewModel,
    initialStereoMode: String,
    useSurfaceEntityRendering: Boolean,
    onStereoModeChange: (String) -> Unit,
    onPlayerViewCreated: (PlayerView) -> Unit,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentStereoMode by remember { mutableStateOf(initialStereoMode) }

    // Dialog state
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Controls visibility with auto-hide
    var controlsVisible by remember { mutableStateOf(true) }
    var hideTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Reset the auto-hide timer whenever controls become visible
    fun resetAutoHide() {
        hideTimestamp = System.currentTimeMillis()
    }

    // Auto-hide controls after 10 seconds of inactivity
    LaunchedEffect(controlsVisible, hideTimestamp) {
        if (controlsVisible) {
            delay(10_000L)
            controlsVisible = false
        }
    }

    // Poll player state
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = viewModel.player.currentPosition
            duration = viewModel.player.duration.coerceAtLeast(0L)
            isPlaying = viewModel.player.isPlaying
            delay(500)
        }
    }

    // Periodically report playback progress
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updatePlaybackProgress()
            delay(5000L)
        }
    }

    // Media session lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.player.playWhenReady = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showPlayerView = !useSurfaceEntityRendering || currentStereoMode == "mono"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                controlsVisible = !controlsVisible
                if (controlsVisible) resetAutoHide()
            },
    ) {
        if (showPlayerView) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = false
                        onPlayerViewCreated(this)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Controls overlay with fade animation
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(24.dp),
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Left: back + title
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_back),
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.currentItemTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Right: track selection buttons
                if (currentStereoMode != "mono") {
                    Text(
                        text = stereoModeDisplayName(currentStereoMode),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF4FC3F7),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(onClick = { showAudioDialog = true; resetAutoHide() }) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_speaker),
                        contentDescription = stringResource(LocalR.string.select_audio_track),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { showSubtitleDialog = true; resetAutoHide() }) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_closed_caption),
                        contentDescription = stringResource(LocalR.string.select_subtitle_track),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { showSpeedDialog = true; resetAutoHide() }) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_gauge),
                        contentDescription = stringResource(LocalR.string.select_playback_speed),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status text
            if (uiState.fileLoaded) {
                Text(
                    text = when {
                        currentStereoMode != "mono" && useSurfaceEntityRendering -> "Spatial 3D Video (${stereoModeDisplayName(currentStereoMode)})"
                        currentStereoMode != "mono" -> "3D Video (${stereoModeDisplayName(currentStereoMode)})"
                        else -> "Video Player"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Seek bar
            if (duration > 0) {
                val progress = currentPosition.toFloat() / duration.toFloat()
                var sliderPosition by remember { mutableFloatStateOf(progress) }
                var isSeeking by remember { mutableStateOf(false) }
                if (!isSeeking) sliderPosition = progress

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Slider(
                        value = sliderPosition,
                        onValueChange = { isSeeking = true; sliderPosition = it; resetAutoHide() },
                        onValueChangeFinished = {
                            viewModel.player.seekTo((sliderPosition * duration).toLong())
                            isSeeking = false
                            resetAutoHide()
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback controls row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Previous track
                if (viewModel.player.hasPreviousMediaItem()) {
                    IconButton(onClick = { viewModel.player.seekToPreviousMediaItem(); resetAutoHide() }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_skip_back),
                            contentDescription = stringResource(LocalR.string.player_controls_skip_back),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Seek back
                IconButton(onClick = { viewModel.player.seekBack(); resetAutoHide() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_rewind),
                        contentDescription = "Seek back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Play/Pause
                FilledIconButton(
                    onClick = { if (isPlaying) viewModel.player.pause() else viewModel.player.play(); resetAutoHide() },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_xr_pause else R.drawable.ic_xr_play),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Seek forward
                IconButton(onClick = { viewModel.player.seekForward(); resetAutoHide() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_forward),
                        contentDescription = "Seek forward",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Next track
                if (viewModel.player.hasNextMediaItem()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.player.seekToNextMediaItem(); resetAutoHide() }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_skip_forward),
                            contentDescription = stringResource(LocalR.string.player_controls_skip_forward),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 3D mode toggle
                TextButton(
                    onClick = {
                        val modes = listOf("mono", "sbs", "top_bottom")
                        val currentIndex = modes.indexOf(currentStereoMode)
                        val nextMode = modes[(currentIndex + 1) % modes.size]
                        currentStereoMode = nextMode
                        onStereoModeChange(nextMode)
                        resetAutoHide()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_3d),
                        contentDescription = "Stereo mode",
                        tint = if (currentStereoMode != "mono") Color(0xFF4FC3F7) else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stereoModeDisplayName(currentStereoMode),
                        color = if (currentStereoMode != "mono") Color(0xFF4FC3F7) else Color.White,
                    )
                }
            }
        }
        } // AnimatedVisibility
    }

    // Dialogs
    if (showAudioDialog) {
        TrackSelectionDialog(
            title = stringResource(LocalR.string.select_audio_track),
            player = viewModel.player,
            trackType = C.TRACK_TYPE_AUDIO,
            onTrackSelected = { index -> viewModel.switchToTrack(C.TRACK_TYPE_AUDIO, index) },
            onDismiss = { showAudioDialog = false },
        )
    }

    if (showSubtitleDialog) {
        TrackSelectionDialog(
            title = stringResource(LocalR.string.select_subtitle_track),
            player = viewModel.player,
            trackType = C.TRACK_TYPE_TEXT,
            onTrackSelected = { index -> viewModel.switchToTrack(C.TRACK_TYPE_TEXT, index) },
            onDismiss = { showSubtitleDialog = false },
        )
    }

    if (showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = viewModel.playbackSpeed,
            onSpeedSelected = { speed -> viewModel.selectSpeed(speed) },
            onDismiss = { showSpeedDialog = false },
        )
    }
}

// endregion

// region Dialogs

@Composable
private fun TrackSelectionDialog(
    title: String,
    player: Player,
    trackType: @C.TrackType Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val trackGroups = player.currentTracks.groups.filter { it.type == trackType && it.isSupported }
    val trackNames = trackGroups.getTrackNames()
    val selectedIndex = trackGroups.indexOfFirst { it.isSelected }
    val noneLabel = stringResource(LocalR.string.none)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // "None" option (index -1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(-1); onDismiss() }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = selectedIndex == -1,
                        onClick = { onTrackSelected(-1); onDismiss() },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(noneLabel)
                }

                // Track options
                trackNames.forEachIndexed { index, name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackSelected(index); onDismiss() }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onTrackSelected(index); onDismiss() },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    val speedLabels = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(LocalR.string.select_playback_speed)) },
        text = {
            Column {
                speeds.forEachIndexed { index, speed ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed); onDismiss() }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedSelected(speed); onDismiss() },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(speedLabels[index])
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

// endregion

// region Utilities

private fun stereoModeDisplayName(mode: String): String {
    return when (mode) {
        "sbs" -> "3D SBS"
        "top_bottom" -> "3D T/B"
        else -> "2D"
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

// endregion
