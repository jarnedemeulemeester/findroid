package dev.jdtech.jellyfin.player.xr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.SurfaceEntity
import dev.jdtech.jellyfin.player.local.domain.getTrackNames
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.player.local.R as LocalR

/**
 * SpatialPlayerScreen implements a fully immersive XR playback experience.
 * It separates the video content (a high-fidelity SurfaceEntity in space)
 * from the playback controls (a floating interactive Spatial Panel).
 */
@Composable
fun SpatialPlayerScreen(
    viewModel: PlayerViewModel,
    session: Session,
    initialStereoMode: String,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentStereoMode by remember { mutableStateOf(initialStereoMode) }

    // Controls visibility with auto-hide logic
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var hideTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun resetAutoHide() {
        hideTimestamp = System.currentTimeMillis()
    }

    LaunchedEffect(controlsVisible, hideTimestamp, isPlaying) {
        if (controlsVisible && isPlaying && !isLocked) {
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
            viewModel.updateCurrentSegment()
            delay(500)
        }
    }

    // IMAX Preset & Video Entity Management
    val videoEntity = remember { mutableStateOf<SurfaceEntity?>(null) }
    
    DisposableEffect(session) {
        // Create the high-fidelity video entity
        val imaxPose = Pose(Vector3(0f, 0f, -3.5f), Quaternion.Identity) // 3.5m away
        val initialShape = SurfaceEntity.Shape.Quad(FloatSize2d(5.0f, 2.8125f)) // Large immersive screen
        
        try {
            val entity = SurfaceEntity.create(
                session = session,
                pose = imaxPose,
                shape = initialShape,
                stereoMode = mapStereoMode(currentStereoMode) ?: SurfaceEntity.StereoMode.MONO
            )
            videoEntity.value = entity
            
            // Connect player to the spatial surface
            viewModel.player.setVideoSurface(entity.getSurface())
        } catch (e: Exception) {
            // Fallback or error handling
        }

        onDispose {
            videoEntity.value?.dispose()
            videoEntity.value = null
        }
    }

    // Update stereo mode when changed
    LaunchedEffect(currentStereoMode) {
        videoEntity.value?.let { entity ->
            // In a real implementation, we might need to recreate the entity
            // but for now let's focus on UI correctness.
        }
    }

    // Update aspect ratio based on video size
    LaunchedEffect(viewModel.player.videoSize) {
        val videoSize = viewModel.player.videoSize
        if (videoSize.width > 0 && videoSize.height > 0) {
            val width = videoSize.width.toFloat()
            val height = videoSize.height.toFloat()
            var aspectRatio = width / height
            
            // Correct aspect ratio for stereo modes
            if (currentStereoMode == "sbs" && aspectRatio > 3.0f) {
                aspectRatio /= 2f
            } else if (currentStereoMode == "top_bottom" && height > width) {
                aspectRatio *= 2f
            }

            val quadWidth = 5.0f // Constant immersive width
            val quadHeight = quadWidth / aspectRatio
            videoEntity.value?.shape = SurfaceEntity.Shape.Quad(FloatSize2d(quadWidth, quadHeight))
        }
    }

    Subspace {
        // The Control Panel: A separate interactive panel floating below the video
        // Enlarged and brought closer for better legibility and reachability.
        SpatialPanel(
            modifier = SubspaceModifier
                .width(1280.dp)
                .height(450.dp)
                .offset(y = (-1100).dp, z = (-2000).dp),
            dragPolicy = MovePolicy(),
            resizePolicy = ResizePolicy()
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ControlPanelUI(
                    viewModel = viewModel,
                    uiState = uiState,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    currentStereoMode = currentStereoMode,
                    isLocked = isLocked,
                    onStereoModeChange = { 
                        currentStereoMode = it
                    },
                    onLockToggle = { isLocked = !isLocked },
                    onBackClick = onBackClick,
                    onInteraction = { resetAutoHide() }
                )
            }
            
            // Interaction surface to summon controls when hidden
            if (!controlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            controlsVisible = true
                            resetAutoHide()
                        }
                )
            }
        }

        // Contextual Skip Button: Enlarged for easier spatial targeting.
        uiState.currentSegment?.let { segment ->
            SpatialPanel(
                modifier = SubspaceModifier
                    .width(320.dp)
                    .height(100.dp)
                    .offset(x = 1600.dp, y = (-700).dp, z = (-1900).dp),
                dragPolicy = MovePolicy()
            ) {
                Surface(
                    onClick = { viewModel.skipSegment(segment); resetAutoHide() },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_skip_forward), 
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(uiState.currentSkipButtonStringRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanelUI(
    viewModel: PlayerViewModel,
    uiState: PlayerViewModel.UiState,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    currentStereoMode: String,
    isLocked: Boolean,
    onStereoModeChange: (String) -> Unit,
    onLockToggle: () -> Unit,
    onBackClick: () -> Unit,
    onInteraction: () -> Unit,
) {
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.Black.copy(alpha = 0.85f),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            // Top Row: Title and Secondary Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(56.dp)) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_left), 
                        contentDescription = "Back", 
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.currentItemTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isLocked) "Controls Locked" else "Spatial Playback",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                if (!isLocked) {
                    IconButton(onClick = { showAudioDialog = true; onInteraction() }, modifier = Modifier.size(56.dp)) {
                        Icon(painterResource(CoreR.drawable.ic_speaker), null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { showSubtitleDialog = true; onInteraction() }, modifier = Modifier.size(56.dp)) {
                        Icon(painterResource(CoreR.drawable.ic_closed_caption), null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { showSpeedDialog = true; onInteraction() }, modifier = Modifier.size(56.dp)) {
                        Icon(painterResource(CoreR.drawable.ic_gauge), null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                
                IconButton(onClick = { onLockToggle(); onInteraction() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        painter = painterResource(if (isLocked) CoreR.drawable.ic_lock else CoreR.drawable.ic_unlock),
                        contentDescription = "Lock Controls",
                        tint = if (isLocked) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Progress Section
            if (!isLocked) {
                ProgressSection(
                    viewModel = viewModel,
                    currentPosition = currentPosition,
                    duration = duration,
                    onInteraction = onInteraction
                )
            }

            Spacer(Modifier.height(24.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLocked) {
                    IconButton(onClick = { viewModel.player.seekBack(); onInteraction() }, modifier = Modifier.size(64.dp)) {
                        Icon(painterResource(CoreR.drawable.ic_rewind), null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.width(32.dp))
                }

                FilledIconButton(
                    onClick = { 
                        if (isPlaying) viewModel.player.pause() else viewModel.player.play()
                        onInteraction()
                    },
                    modifier = Modifier.size(96.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) CoreR.drawable.ic_pause else CoreR.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }

                if (!isLocked) {
                    Spacer(Modifier.width(32.dp))
                    IconButton(onClick = { viewModel.player.seekForward(); onInteraction() }, modifier = Modifier.size(64.dp)) {
                        Icon(painterResource(CoreR.drawable.ic_fast_forward), null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    
                    Spacer(Modifier.width(64.dp))
                    
                    TextButton(
                        onClick = {
                            val modes = listOf("mono", "sbs", "top_bottom")
                            val next = modes[(modes.indexOf(currentStereoMode) + 1) % modes.size]
                            onStereoModeChange(next)
                            onInteraction()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            painterResource(CoreR.drawable.ic_3d), 
                            null, 
                            tint = if (currentStereoMode != "mono") Color(0xFF4FC3F7) else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stereoModeDisplayName(currentStereoMode), 
                            style = MaterialTheme.typography.titleMedium,
                            color = if (currentStereoMode != "mono") Color(0xFF4FC3F7) else Color.White
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAudioDialog) {
        TrackSelectionDialog(
            title = stringResource(LocalR.string.select_audio_track),
            player = viewModel.player,
            trackType = C.TRACK_TYPE_AUDIO,
            onTrackSelected = { index -> viewModel.switchToTrack(C.TRACK_TYPE_AUDIO, index) },
            onDismiss = { showAudioDialog = false }
        )
    }
    if (showSubtitleDialog) {
        TrackSelectionDialog(
            title = stringResource(LocalR.string.select_subtitle_track),
            player = viewModel.player,
            trackType = C.TRACK_TYPE_TEXT,
            onTrackSelected = { index -> viewModel.switchToTrack(C.TRACK_TYPE_TEXT, index) },
            onDismiss = { showSubtitleDialog = false }
        )
    }
    if (showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = viewModel.playbackSpeed,
            onSpeedSelected = { speed -> viewModel.selectSpeed(speed) },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
private fun ProgressSection(
    viewModel: PlayerViewModel,
    currentPosition: Long,
    duration: Long,
    onInteraction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    if (!isDragging) sliderValue = progress

    Column {
        // Trickplay Preview Thumbnail: Enlarged for spatial depth
        if (isDragging && uiState.currentTrickplay != null) {
            val trickplay = uiState.currentTrickplay!!
            val totalThumbnails = trickplay.images.size
            val index = (sliderValue * (totalThumbnails - 1)).toInt().coerceIn(0, totalThumbnails - 1)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = trickplay.images[index].asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Slider(
                value = sliderValue,
                onValueChange = { 
                    isDragging = true
                    sliderValue = it
                    onInteraction()
                },
                onValueChangeFinished = {
                    viewModel.player.seekTo((sliderValue * duration).toLong())
                    isDragging = false
                    onInteraction()
                },
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )
            Text(formatTime(duration), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

// Reuse Dialogs and Utils from XrPlayerActivity with minor adjustments if needed
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTrackSelected(-1); onDismiss() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedIndex == -1, onClick = { onTrackSelected(-1); onDismiss() })
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(LocalR.string.none), style = MaterialTheme.typography.bodyLarge)
                }
                trackNames.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onTrackSelected(index); onDismiss() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = { onTrackSelected(index); onDismiss() })
                        Spacer(Modifier.width(12.dp))
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", style = MaterialTheme.typography.labelLarge) } }
    )
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(LocalR.string.select_playback_speed), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSpeedSelected(speed); onDismiss() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSpeed == speed, onClick = { onSpeedSelected(speed); onDismiss() })
                        Spacer(Modifier.width(12.dp))
                        Text("${speed}x", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", style = MaterialTheme.typography.labelLarge) } }
    )
}

private fun mapStereoMode(mode: String): SurfaceEntity.StereoMode? {
    return when (mode) {
        "sbs" -> SurfaceEntity.StereoMode.SIDE_BY_SIDE
        "top_bottom" -> SurfaceEntity.StereoMode.TOP_BOTTOM
        else -> null
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
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}
