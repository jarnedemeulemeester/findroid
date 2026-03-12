package dev.jdtech.jellyfin.player.xr

import android.util.TypedValue
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import dev.jdtech.jellyfin.player.local.domain.getTrackNames
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import kotlinx.coroutines.delay
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.player.local.R as LocalR

/**
 * SpatialPlayerScreen — IMAX-style immersive XR playback experience.
 *
 * Architecture:
 *  - SurfaceEntity (SceneCore): high-fidelity video surface, user-movable
 *  - Subtitle SpatialPanel: perspective-scaled, follows video pose via state
 *  - Control SpatialPanel: floats below the video with:
 *      • Orbiter (End): secondary controls (audio / subtitle / speed)
 *      • Orbiter (Bottom): reveal button when controls are hidden
 *      • SpatialDialog: track & speed selection (pushes panel back 125dp for depth)
 *  - Skip SpatialPanel: contextual, right of controls
 */
@Composable
fun SpatialPlayerScreen(
    viewModel: PlayerViewModel,
    session: Session,
    initialStereoMode: String,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val xrSubtitleSize = viewModel.appPreferences.getValue(viewModel.appPreferences.xrSubtitleSize).toFloat()

    // Build caption style from user preferences so the selected colours / background
    // are honoured instead of the system default (which renders a black background box).
    val subtitleTextColor = viewModel.appPreferences.getValue(viewModel.appPreferences.subtitleTextColor)
    val subtitleBackgroundColor = viewModel.appPreferences.getValue(viewModel.appPreferences.subtitleBackgroundColor)
    val captionStyle = CaptionStyleCompat(
        subtitleTextColor,
        subtitleBackgroundColor,
        android.graphics.Color.TRANSPARENT, // no window background
        CaptionStyleCompat.EDGE_TYPE_NONE,
        android.graphics.Color.WHITE,       // edge colour (unused with EDGE_TYPE_NONE)
        null,                               // typeface — use system default
    )

    // --- Spatial Audio capability (reactive) ---
    var spatialAudioAvailable by remember {
        mutableStateOf(session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_AUDIO))
    }
    DisposableEffect(session) {
        val listener = java.util.function.Consumer<Set<SpatialCapability>> { caps ->
            spatialAudioAvailable = caps.contains(SpatialCapability.SPATIAL_AUDIO)
            Timber.d("XR_AUDIO: spatial audio available=$spatialAudioAvailable")
        }
        session.scene.addSpatialCapabilitiesChangedListener(listener)
        onDispose { session.scene.removeSpatialCapabilitiesChangedListener(listener) }
    }

    // --- Player state ---
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentStereoMode by remember { mutableStateOf(initialStereoMode) }
    var videoWidth by remember { mutableFloatStateOf(10.0f) }
    var videoHeight by remember { mutableFloatStateOf(5.625f) }
    var currentCues by remember { mutableStateOf<List<Cue>>(emptyList()) }

    // --- Controls UI state ---
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var hideTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // --- Dialog state (lifted here so SpatialDialog lives inside the control SpatialPanel) ---
    var activeDialog by remember { mutableStateOf<String?>(null) }

    fun resetAutoHide() {
        hideTimestamp = System.currentTimeMillis()
    }

    // Auto-hide controls after 10 s during playback
    LaunchedEffect(controlsVisible, hideTimestamp, isPlaying) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(10_000L)
            controlsVisible = false
        }
    }

    // Immersive environment: blackout during playback, passthrough when paused
    LaunchedEffect(isPlaying) {
        val environment = session.scene.spatialEnvironment
        try {
            environment.preferredSpatialEnvironment = if (isPlaying) {
                SpatialEnvironment.SpatialEnvironmentPreference(skybox = null, geometry = null)
            } else {
                null
            }
        } catch (_: Exception) {}
    }

    // Subtitle cue listener
    DisposableEffect(viewModel.player) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                currentCues = cueGroup.cues
            }
        }
        viewModel.player.addListener(listener)
        onDispose { viewModel.player.removeListener(listener) }
    }

    // Poll playback state (position, duration, isPlaying, segments)
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = viewModel.player.currentPosition
            duration = viewModel.player.duration.coerceAtLeast(0L)
            isPlaying = viewModel.player.isPlaying
            viewModel.updateCurrentSegment()
            delay(500)
        }
    }

    // --- SceneCore video entity ---
    var videoPose by remember { mutableStateOf(Pose(Vector3(0f, 0f, -5.0f), Quaternion.Identity)) }
    val videoEntity = remember { mutableStateOf<SurfaceEntity?>(null) }

    DisposableEffect(session) {
        val initialShape = SurfaceEntity.Shape.Quad(FloatSize2d(10.0f, 5.625f))
        try {
            val entity = SurfaceEntity.create(
                session = session,
                pose = videoPose,
                shape = initialShape,
                stereoMode = mapStereoMode(currentStereoMode) ?: SurfaceEntity.StereoMode.MONO,
            )
            val movable = androidx.xr.scenecore.MovableComponent.createSystemMovable(session)
            entity.addComponent(movable)
            // Update Compose state immediately when the user moves the screen — all child
            // panels react synchronously via state recomposition.
            movable.addMoveListener(object : androidx.xr.scenecore.EntityMoveListener {
                override fun onMoveUpdate(
                    movedEntity: androidx.xr.scenecore.Entity,
                    ray: androidx.xr.runtime.math.Ray,
                    pose: Pose,
                    scale: Float,
                ) {
                    videoPose = pose
                }
            })
            videoEntity.value = entity
            viewModel.player.setVideoSurface(entity.getSurface())
        } catch (_: Exception) {}

        onDispose {
            videoEntity.value?.dispose()
            videoEntity.value = null
            try { session.scene.spatialEnvironment.preferredSpatialEnvironment = null } catch (_: Exception) {}
        }
    }

    // Update SurfaceEntity shape when video dimensions change
    LaunchedEffect(viewModel.player.videoSize) {
        val videoSize = viewModel.player.videoSize
        if (videoSize.width > 0 && videoSize.height > 0) {
            var aspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
            if (currentStereoMode == "sbs" && aspectRatio > 3.0f) aspectRatio /= 2f
            else if (currentStereoMode == "top_bottom" && videoSize.height > videoSize.width) aspectRatio *= 2f
            val quadWidth = 10.0f
            val quadHeight = quadWidth / aspectRatio
            videoWidth = quadWidth
            videoHeight = quadHeight
            videoEntity.value?.shape = SurfaceEntity.Shape.Quad(FloatSize2d(quadWidth, quadHeight))
        }
    }

    // --- Layout calculations ---
    val videoDepth = 5.0f
    val subtitleDepth = 2.0f
    val subtitleScaleFactor = subtitleDepth / videoDepth
    val scaledVideoWidthDp = videoWidth * subtitleScaleFactor * 1000f
    val scaledVideoHeightDp = videoHeight * subtitleScaleFactor * 1000f
    val subtitlePanelWidthDp = scaledVideoWidthDp.coerceAtMost(2500f)
    val subtitlePanelHeightDp = scaledVideoHeightDp.coerceAtMost(2500f)
    val finalSubtitleSize = xrSubtitleSize * (subtitlePanelHeightDp / 600f).coerceAtLeast(1f)
    // Controls sit just below the video surface
    val controlsPanelY = -(scaledVideoHeightDp / 2f + 400f)

    Subspace {
        // ── Subtitle Panel ──────────────────────────────────────────────────────────
        // Spans the full projected video area so PGS cue positions map correctly.
        SpatialPanel(
            modifier = SubspaceModifier
                .width(subtitlePanelWidthDp.dp)
                .height(subtitlePanelHeightDp.dp)
                .offset(
                    x = (videoPose.translation.x * 1000f).dp,
                    y = (videoPose.translation.y * 1000f).dp,
                    z = (videoPose.translation.z * 1000f).dp,
                )
                .rotate(quaternion = videoPose.rotation)
                .offset(y = 0.dp, z = 3000.dp),
        ) {
            AndroidView(
                factory = { context ->
                    SubtitleView(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        // 4 % bottom padding keeps text off the very edge of the panel,
                        // matching the TV / cinema convention without pushing it to the middle.
                        setBottomPaddingFraction(0.04f)
                        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, finalSubtitleSize)
                        // Apply user-selected style — do NOT call setUserDefaultStyle() here:
                        // that method overrides our fixed text size with a fractional one and
                        // forces the OS system caption style (black background box).
                        setStyle(captionStyle)
                    }
                },
                update = { view ->
                    // Re-apply style and size on every update to prevent the OS from
                    // silently re-applying the system caption style (GEMINI.md guidance).
                    view.setStyle(captionStyle)
                    view.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, finalSubtitleSize)
                    // Selective bottom-anchoring for the IMAX experience:
                    //
                    //  • Unpositioned cues (SRT/VTT, lineType == TYPE_UNSET):
                    //    left as-is — SubtitleView places them at the bottom via
                    //    setBottomPaddingFraction. ✓
                    //
                    //  • Top-positioned cues (line < 0.15, e.g. signs/credits at top):
                    //    left as-is — respect intentional top placement. ✓
                    //
                    //  • Bottom-positioned cues (line > 0.85, e.g. PGS for movies):
                    //    left as-is — already near the bottom, movies display correctly. ✓
                    //
                    //  • Middle-zone cues (0.15 ≤ line < 0.85, LINE_TYPE_FRACTION):
                    //    Media3's ASS/SSA decoder can produce these for anime dialogue
                    //    that should visually appear at the bottom.  Push them down.
                    val processedCues = currentCues.map { cue ->
                        val isMidZone = cue.lineType == Cue.LINE_TYPE_FRACTION &&
                            cue.line >= 0.15f && cue.line < 0.85f
                        if (isMidZone) {
                            cue.buildUpon()
                                .setLine(0.95f, Cue.LINE_TYPE_FRACTION)
                                .setLineAnchor(Cue.ANCHOR_TYPE_END)
                                .build()
                        } else {
                            cue
                        }
                    }
                    view.setCues(processedCues)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Control Panel ────────────────────────────────────────────────────────────
        // Floats below the video.  Secondary controls are in an Orbiter on the right
        // so the main panel stays uncluttered (IMAX principle: screen first, UI second).
        SpatialPanel(
            modifier = SubspaceModifier
                .width(1400.dp)
                .height(600.dp)
                .offset(
                    x = (videoPose.translation.x * 1000f).dp,
                    y = (videoPose.translation.y * 1000f).dp,
                    z = (videoPose.translation.z * 1000f).dp,
                )
                .rotate(quaternion = videoPose.rotation)
                .offset(x = 0.dp, y = controlsPanelY.dp, z = 3000.dp),
            resizePolicy = ResizePolicy(),
        ) {
            // ── Orbiter: secondary controls (right edge, hidden when locked/controls hidden) ──
            if (controlsVisible && !isLocked) {
                Orbiter(
                    position = ContentEdge.End,
                    alignment = Alignment.CenterVertically,
                    offset = 20.dp,
                ) {
                    SecondaryControlsOrbiter(
                        onAudioClick = { activeDialog = "audio"; resetAutoHide() },
                        onSubtitleClick = { activeDialog = "subtitle"; resetAutoHide() },
                        onSpeedClick = { activeDialog = "speed"; resetAutoHide() },
                    )
                }
            }

            // ── Orbiter: reveal button (bottom edge, only when controls hidden) ──
            if (!controlsVisible) {
                Orbiter(
                    position = ContentEdge.Bottom,
                    alignment = Alignment.CenterHorizontally,
                    offset = 40.dp,
                ) {
                    IconButton(
                        onClick = { controlsVisible = true; resetAutoHide() },
                        modifier = Modifier.size(80.dp),
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_eye),
                            contentDescription = "Show Controls",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
            }

            // ── Main control content ──────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
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
                        spatialAudioAvailable = spatialAudioAvailable,
                        onStereoModeChange = { currentStereoMode = it },
                        onLockToggle = { isLocked = !isLocked },
                        onHideClick = { controlsVisible = false },
                        onBackClick = onBackClick,
                        resetAutoHide = { resetAutoHide() },
                    )
                }

                if (!controlsVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { controlsVisible = true; resetAutoHide() },
                            ),
                    )
                }
            }

            // ── SpatialDialogs ────────────────────────────────────────────────────
            // Placed inside the control SpatialPanel: when shown, the SDK pushes the
            // panel back 125 dp and renders the dialog floating in front — proper XR
            // depth hierarchy without manual zIndex hacks.
            if (activeDialog == "audio") {
                SpatialDialog(onDismissRequest = { activeDialog = null }) {
                    TrackSelectionDialogContent(
                        title = stringResource(LocalR.string.select_audio_track),
                        player = viewModel.player,
                        trackType = C.TRACK_TYPE_AUDIO,
                        onTrackSelected = { index -> viewModel.switchToTrack(C.TRACK_TYPE_AUDIO, index) },
                        onDismiss = { activeDialog = null },
                    )
                }
            }
            if (activeDialog == "subtitle") {
                SpatialDialog(onDismissRequest = { activeDialog = null }) {
                    TrackSelectionDialogContent(
                        title = stringResource(LocalR.string.select_subtitle_track),
                        player = viewModel.player,
                        trackType = C.TRACK_TYPE_TEXT,
                        onTrackSelected = { index -> viewModel.switchToTrack(C.TRACK_TYPE_TEXT, index) },
                        onDismiss = { activeDialog = null },
                    )
                }
            }
            if (activeDialog == "speed") {
                SpatialDialog(onDismissRequest = { activeDialog = null }) {
                    SpeedDialogContent(
                        currentSpeed = viewModel.playbackSpeed,
                        onSpeedSelected = { speed -> viewModel.selectSpeed(speed) },
                        onDismiss = { activeDialog = null },
                    )
                }
            }
        }

        // ── Contextual Skip Panel ────────────────────────────────────────────────
        uiState.currentSegment?.let { segment ->
            SpatialPanel(
                modifier = SubspaceModifier
                    .width(360.dp)
                    .height(120.dp)
                    .offset(
                        x = (videoPose.translation.x * 1000f).dp,
                        y = (videoPose.translation.y * 1000f).dp,
                        z = (videoPose.translation.z * 1000f).dp,
                    )
                    .rotate(quaternion = videoPose.rotation)
                    .offset(x = 950.dp, y = controlsPanelY.dp, z = 3000.dp),
            ) {
                Surface(
                    onClick = { viewModel.skipSegment(segment); resetAutoHide() },
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_skip_forward),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(uiState.currentSkipButtonStringRes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ── Secondary Controls Orbiter ────────────────────────────────────────────────────
// Floats to the right of the control panel. Keeps the main panel clean while still
// giving one-glance access to audio, subtitle, and speed.
@Composable
private fun SecondaryControlsOrbiter(
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onSpeedClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(40.dp),
        color = Color.Black.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onAudioClick, modifier = Modifier.size(80.dp)) {
                Icon(
                    painterResource(CoreR.drawable.ic_speaker),
                    contentDescription = "Audio track",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
            IconButton(onClick = onSubtitleClick, modifier = Modifier.size(80.dp)) {
                Icon(
                    painterResource(CoreR.drawable.ic_closed_caption),
                    contentDescription = "Subtitle track",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
            IconButton(onClick = onSpeedClick, modifier = Modifier.size(80.dp)) {
                Icon(
                    painterResource(CoreR.drawable.ic_gauge),
                    contentDescription = "Playback speed",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}

// ── Control Panel UI ──────────────────────────────────────────────────────────────
// Simplified — audio/subtitle/speed moved to the right-side Orbiter so this surface
// stays focused on the core playback experience.
@Composable
private fun ControlPanelUI(
    viewModel: PlayerViewModel,
    uiState: PlayerViewModel.UiState,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    currentStereoMode: String,
    isLocked: Boolean,
    spatialAudioAvailable: Boolean,
    onStereoModeChange: (String) -> Unit,
    onLockToggle: () -> Unit,
    onHideClick: () -> Unit,
    onBackClick: () -> Unit,
    resetAutoHide: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(48.dp),
        color = Color.Black.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.padding(40.dp)) {
            // ── Top row: back / title / indicator / lock / hide ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(80.dp)) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_left),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.currentItemTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            isLocked -> "Controls Locked"
                            spatialAudioAvailable -> "Spatial Playback • Spatial Audio"
                            else -> "Spatial Playback"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (spatialAudioAvailable && !isLocked)
                            Color(0xFF4FC3F7).copy(alpha = 0.8f)
                        else
                            Color.White.copy(alpha = 0.6f),
                    )
                }
                if (!isLocked) {
                    IconButton(onClick = { onHideClick() }, modifier = Modifier.size(80.dp)) {
                        Icon(
                            painterResource(CoreR.drawable.ic_eye_off),
                            contentDescription = "Hide Controls",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                IconButton(
                    onClick = { onLockToggle(); resetAutoHide() },
                    modifier = Modifier.size(80.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isLocked) CoreR.drawable.ic_lock else CoreR.drawable.ic_unlock,
                        ),
                        contentDescription = "Lock Controls",
                        tint = if (isLocked) Color.Red else Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (!isLocked) {
                ProgressSection(
                    viewModel = viewModel,
                    currentPosition = currentPosition,
                    duration = duration,
                    resetAutoHide = resetAutoHide,
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Playback controls ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLocked) {
                    IconButton(
                        onClick = { viewModel.player.seekBack(); resetAutoHide() },
                        modifier = Modifier.size(96.dp),
                    ) {
                        Icon(
                            painterResource(CoreR.drawable.ic_rewind),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    Spacer(Modifier.width(48.dp))
                }

                FilledIconButton(
                    onClick = {
                        if (isPlaying) viewModel.player.pause() else viewModel.player.play()
                        resetAutoHide()
                    },
                    modifier = Modifier.size(120.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) CoreR.drawable.ic_pause else CoreR.drawable.ic_play,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                    )
                }

                if (!isLocked) {
                    Spacer(Modifier.width(48.dp))
                    IconButton(
                        onClick = { viewModel.player.seekForward(); resetAutoHide() },
                        modifier = Modifier.size(96.dp),
                    ) {
                        Icon(
                            painterResource(CoreR.drawable.ic_fast_forward),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    Spacer(Modifier.width(80.dp))
                    // 2D / 3D stereo mode toggle
                    TextButton(
                        onClick = {
                            val modes = listOf("mono", "sbs", "top_bottom")
                            val next = modes[(modes.indexOf(currentStereoMode) + 1) % modes.size]
                            onStereoModeChange(next)
                            resetAutoHide()
                        },
                        modifier = Modifier.height(80.dp),
                    ) {
                        Icon(
                            painterResource(CoreR.drawable.ic_3d),
                            contentDescription = null,
                            tint = if (currentStereoMode != "mono") Color(0xFF4FC3F7) else Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            stereoModeDisplayName(currentStereoMode),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (currentStereoMode != "mono") Color(0xFF4FC3F7) else Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ── Track Selection Dialog ─────────────────────────────────────────────────────────
@Composable
private fun TrackSelectionDialogContent(
    title: String,
    player: Player,
    trackType: @C.TrackType Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val trackGroups = player.currentTracks.groups.filter { it.type == trackType && it.isSupported }
    val trackNames = trackGroups.getTrackNames()
    val selectedIndex = trackGroups.indexOfFirst { it.isSelected }

    Surface(
        modifier = Modifier.width(600.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(-1); onDismiss() }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedIndex == -1,
                        onClick = { onTrackSelected(-1); onDismiss() },
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(LocalR.string.none), style = MaterialTheme.typography.titleLarge)
                }
                trackNames.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackSelected(index); onDismiss() }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onTrackSelected(index); onDismiss() },
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("CLOSE", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Speed Selection Dialog ────────────────────────────────────────────────────────
@Composable
private fun SpeedDialogContent(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    Surface(
        modifier = Modifier.width(400.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text(
                stringResource(LocalR.string.select_playback_speed),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedSelected(speed); onDismiss() },
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("${speed}x", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("CLOSE", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Progress / Trickplay ──────────────────────────────────────────────────────────
@Composable
private fun ProgressSection(
    viewModel: PlayerViewModel,
    currentPosition: Long,
    duration: Long,
    resetAutoHide: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    if (!isDragging) sliderValue = progress

    Column {
        if (isDragging && uiState.currentTrickplay != null) {
            val trickplay = uiState.currentTrickplay!!
            val totalThumbnails = trickplay.images.size
            val index = (sliderValue * (totalThumbnails - 1)).toInt().coerceIn(0, totalThumbnails - 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = trickplay.images[index].asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatTime(currentPosition),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
            Slider(
                value = sliderValue,
                onValueChange = {
                    isDragging = true
                    sliderValue = it
                    resetAutoHide()
                },
                onValueChangeFinished = {
                    viewModel.player.seekTo((sliderValue * duration).toLong())
                    isDragging = false
                    resetAutoHide()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            )
            Text(
                formatTime(duration),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────────

private fun mapStereoMode(mode: String): SurfaceEntity.StereoMode? = when (mode) {
    "sbs" -> SurfaceEntity.StereoMode.SIDE_BY_SIDE
    "top_bottom" -> SurfaceEntity.StereoMode.TOP_BOTTOM
    else -> null
}

private fun stereoModeDisplayName(mode: String): String = when (mode) {
    "sbs" -> "3D SBS"
    "top_bottom" -> "3D T/B"
    else -> "2D"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}
