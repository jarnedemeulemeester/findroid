package dev.jdtech.jellyfin.player.xr

import android.os.Bundle
import android.view.Surface as AndroidSurface
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Surface
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.xr.scenecore.Session
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import timber.log.Timber

@AndroidEntryPoint
class XrPlayerActivity : AppCompatActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private var xrSession: Session? = null
    private var surfaceEntity: SurfaceEntity? = null
    private var mediaSession: MediaSession? = null
    private var videoSurface: AndroidSurface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = UUID.fromString(intent.extras!!.getString("itemId"))
        val itemKind = intent.extras!!.getString("itemKind") ?: ""
        val startFromBeginning = intent.extras!!.getBoolean("startFromBeginning")
        val stereoModeStr = intent.extras?.getString("stereoMode") ?: "mono"

        // Initialize XR Session
        try {
            xrSession = Session.create(this)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create XR session, XR not available")
            finish()
            return
        }

        // Create the stereoscopic surface
        createSurfaceEntity(stereoModeStr)

        // Route ExoPlayer video output to the XR surface
        val player = viewModel.player
        if (player is ExoPlayer && videoSurface != null) {
            player.setVideoSurface(videoSurface)

            // Listen for video size changes to update aspect ratio
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        updateSurfaceAspectRatio(videoSize, stereoModeStr)
                    }
                }
            })
        } else {
            Timber.w("XR spatial video requires ExoPlayer backend")
        }

        setContent {
            XrPlayerTheme {
                XrPlayerScreen(
                    viewModel = viewModel,
                    initialStereoMode = stereoModeStr,
                    onStereoModeChange = { newMode ->
                        recreateSurfaceEntity(newMode)
                    },
                    onBackClick = {
                        finishPlayback()
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

    private fun createSurfaceEntity(stereoModeStr: String) {
        val session = xrSession ?: return

        val xrStereoMode = mapStereoMode(stereoModeStr)

        // Position the virtual screen 1.5 meters in front of the user
        val pose = Pose(Vector3(0f, 0f, -1.5f))

        try {
            surfaceEntity = if (xrStereoMode != null) {
                // Create a stereoscopic surface entity for 3D content
                // The SurfaceEntity splits the source frame according to the stereo mode
                // and sends each half to the appropriate eye
                SurfaceEntity.create(
                    session = session,
                    stereoMode = xrStereoMode,
                    pose = pose,
                )
            } else {
                // Create a standard (mono) surface entity for 2D content
                SurfaceEntity.create(
                    session = session,
                    pose = pose,
                )
            }
            videoSurface = surfaceEntity?.getSurface()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create SurfaceEntity")
        }
    }

    private fun recreateSurfaceEntity(newStereoMode: String) {
        val player = viewModel.player

        // Clear old surface
        if (player is ExoPlayer) {
            player.clearVideoSurface()
        }

        // Destroy old entity
        surfaceEntity = null
        videoSurface = null

        // Create new entity with updated mode
        createSurfaceEntity(newStereoMode)

        // Reconnect player
        if (player is ExoPlayer && videoSurface != null) {
            player.setVideoSurface(videoSurface)
        }
    }

    private fun updateSurfaceAspectRatio(videoSize: VideoSize, stereoMode: String) {
        val entity = surfaceEntity ?: return

        // For SBS content, actual display width is half the encoded width
        val displayWidth = when (stereoMode) {
            "sbs" -> videoSize.width / 2f
            else -> videoSize.width.toFloat()
        }
        // For TB content, actual display height is half the encoded height
        val displayHeight = when (stereoMode) {
            "top_bottom" -> videoSize.height / 2f
            else -> videoSize.height.toFloat()
        }

        val aspectRatio = displayWidth / displayHeight
        val quadWidth = 2.0f // meters
        val quadHeight = quadWidth / aspectRatio

        Timber.d("Updated XR surface: ${displayWidth}x${displayHeight}, aspect=$aspectRatio, quad=${quadWidth}x${quadHeight}")
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
        val player = viewModel.player
        if (player is ExoPlayer) {
            player.clearVideoSurface()
        }
        surfaceEntity = null
        videoSurface = null
    }

    private fun finishPlayback() {
        val player = viewModel.player
        if (player is ExoPlayer) {
            player.clearVideoSurface()
        }
        finish()
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
    onStereoModeChange: (String) -> Unit,
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.85f),
    ) {
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
                // Stereo mode badge
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

            // Playback info
            if (uiState.fileLoaded) {
                Text(
                    text = "Spatial Video Player",
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
                // Seek back
                IconButton(
                    onClick = {
                        viewModel.player.seekBack()
                    },
                ) {
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
                    onClick = {
                        if (isPlaying) {
                            viewModel.player.pause()
                        } else {
                            viewModel.player.play()
                        }
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

                // Seek forward
                IconButton(
                    onClick = {
                        viewModel.player.seekForward()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xr_forward),
                        contentDescription = "Seek forward",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Stereo mode toggle
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
