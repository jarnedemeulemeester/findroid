package dev.jdtech.jellyfin.player.xr

import android.os.Bundle
import android.view.Surface as AndroidSurface
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
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
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import timber.log.Timber

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
                            // Recreate SurfaceEntity with new stereo mode and reconnect
                            recreateSurfaceEntity()
                            if (isStereo3d() && videoSurface != null) {
                                (viewModel.player as? ExoPlayer)?.setVideoSurface(videoSurface)
                            }
                        } else {
                            // 2D fallback: apply left-eye cropping
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

        // Position the virtual screen 2.0 meters in front of the user
        val pose = Pose(Vector3(0f, 0f, -2.0f))

        // Default 16:9 quad at 2m width
        val shape = SurfaceEntity.Shape.Quad(FloatSize2d(2.0f, 1.125f))

        try {
            surfaceEntity = if (xrStereoMode != null) {
                SurfaceEntity.create(
                    session = session,
                    pose = pose,
                    shape = shape,
                    stereoMode = xrStereoMode,
                )
            } else {
                SurfaceEntity.create(
                    session = session,
                    pose = pose,
                    shape = shape,
                )
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
        val quadWidth = 2.0f // meters
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

@Composable
private fun XrPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content,
    )
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

    // When using SurfaceEntity rendering for 3D, video renders in the XR scene,
    // not in the 2D window. Show PlayerView only when NOT using SurfaceEntity
    // rendering, or when in mono mode.
    val showPlayerView = !useSurfaceEntityRendering || currentStereoMode == "mono"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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

        // Controls overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
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
                if (currentStereoMode != "mono") {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stereoModeDisplayName(currentStereoMode),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF4FC3F7),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.fileLoaded) {
                Text(
                    text = when {
                        currentStereoMode != "mono" && useSurfaceEntityRendering -> "Spatial 3D Video (${stereoModeDisplayName(currentStereoMode)})"
                        currentStereoMode != "mono" -> "3D Video (${stereoModeDisplayName(currentStereoMode)}) - 2D fallback"
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

                if (!isSeeking) {
                    sliderPosition = progress
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Slider(
                        value = sliderPosition,
                        onValueChange = { value ->
                            isSeeking = true
                            sliderPosition = value
                        },
                        onValueChangeFinished = {
                            viewModel.player.seekTo((sliderPosition * duration).toLong())
                            isSeeking = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { viewModel.player.seekBack() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_rewind),
                        contentDescription = "Seek back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = {
                        if (isPlaying) viewModel.player.pause() else viewModel.player.play()
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_xr_pause else R.drawable.ic_xr_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = { viewModel.player.seekForward() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_forward),
                        contentDescription = "Seek forward",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                TextButton(
                    onClick = {
                        val modes = listOf("mono", "sbs", "top_bottom")
                        val currentIndex = modes.indexOf(currentStereoMode)
                        val nextMode = modes[(currentIndex + 1) % modes.size]
                        currentStereoMode = nextMode
                        onStereoModeChange(nextMode)
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
    }
}

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
