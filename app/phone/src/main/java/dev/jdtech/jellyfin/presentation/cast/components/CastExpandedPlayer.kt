package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.cast.CastPlaybackState
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.PlayerMediaType
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastExpandedPlayer(
    castManager: CastManager,
    isExpandedScreen: Boolean, // true for tablets
    posterUrl: String?,
    trickplay: Trickplay?,
    skippableSegment: LongRange?, // [start, end]
    onDeviceClick: () -> Unit,
    onClose: () -> Unit
) {
    if (!isExpandedScreen) {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RectangleShape,
            sheetMaxWidth = Dp.Unspecified
        ) {
            CastExpandedPlayerContent(castManager, posterUrl, trickplay, skippableSegment, onDeviceClick, onClose)
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            CastExpandedPlayerContent(castManager, posterUrl, trickplay, skippableSegment, onDeviceClick, onClose)
        }
    }
}

@Composable
private fun CastExpandedPlayerContent(
    castManager: CastManager,
    posterUrl: String?,
    trickplay: Trickplay?,
    skippableSegment: LongRange?,
    onDeviceClick: () -> Unit,
    onClose: () -> Unit
) {
    val playbackState by castManager.playbackState.collectAsState()
    val audioTracks by castManager.audioTracks.collectAsState()
    val subtitleTracks by castManager.subtitleTracks.collectAsState()
    val connectedDevice by castManager.connectedDevice.collectAsState()
    val volume by castManager.volume.collectAsState()
    val currentItem by castManager.currentItem.collectAsState()

    CastExpandedPlayerContentLayout(
        playbackState = playbackState,
        connectedDeviceName = connectedDevice?.name,
        volume = volume,
        title = currentItem?.name ?: "",
        mediaType = currentItem?.mediaType ?: PlayerMediaType.UNKNOWN,
        trickplayWidth = currentItem?.trickplayInfo?.width,
        trickplayHeight = currentItem?.trickplayInfo?.height,
        chapters = currentItem?.chapters ?: emptyList(),
        audioTracks = audioTracks,
        subtitleTracks = subtitleTracks,
        posterUrl = posterUrl,
        trickplay = trickplay,
        skippableSegment = skippableSegment,
        onPlay = { castManager.play() },
        onPause = { castManager.pause() },
        onSeek = { castManager.seekTo(it) },
        onSetVolume = { castManager.setVolume(it) },
        onSetAudioTrack = { castManager.setAudioTrack(it) },
        onSetSubtitleTrack = { castManager.setSubtitleTrack(it) },
        onDeviceClick = onDeviceClick,
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastExpandedPlayerContentLayout(
    playbackState: CastPlaybackState,
    connectedDeviceName: String?,
    volume: Float,
    title: String,
    mediaType: PlayerMediaType,
    trickplayWidth: Int?,
    trickplayHeight: Int?,
    chapters: List<PlayerChapter>,
    audioTracks: List<Track>,
    subtitleTracks: List<Track>,
    posterUrl: String?,
    trickplay: Trickplay?,
    skippableSegment: LongRange?,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSetAudioTrack: (Track?) -> Unit,
    onSetSubtitleTrack: (Track?) -> Unit,
    onDeviceClick: () -> Unit,
    onClose: () -> Unit
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    
    var showTrackSelection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(painterResource(CoreR.drawable.ic_x), contentDescription = "Close")
            }
            
            // Device Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF81C784),
                onClick = onDeviceClick
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(CoreR.drawable.ic_cast), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(connectedDeviceName ?: "No Device", style = MaterialTheme.typography.labelLarge, color = Color.Black)
                }
            }

            IconButton(
                onClick = { showTrackSelection = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(painterResource(CoreR.drawable.ic_settings), contentDescription = "Settings")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center Image
        val isMovie = mediaType == PlayerMediaType.MOVIE
        val aspectRatio = if (isScrubbing && trickplayWidth != null && trickplayHeight != null) {
            trickplayWidth.toFloat() / trickplayHeight.toFloat()
        } else if (mediaType == PlayerMediaType.EPISODE) {
            16f / 9f
        } else if (isMovie) {
            2f / 3f
        } else {
            16f / 10f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isMovie && !isScrubbing) 64.dp else 24.dp)
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (isScrubbing && trickplay != null && playbackState.duration > 0 && trickplay.images.isNotEmpty()) {
                val index = (scrubPosition.toLong() / trickplay.interval).coerceIn(0L, (trickplay.images.size - 1).toLong()).toInt()
                val bitmap = trickplay.images.getOrNull(index)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Scrub Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Preview missing", color = Color.White)
                }
            } else if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(painterResource(CoreR.drawable.ic_cast), contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Progress Bar
        val currentProgress = if (isScrubbing) scrubPosition else playbackState.currentPosition.toFloat()
        val duration = playbackState.duration.toFloat().coerceAtLeast(1f)
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Slider(
                value = currentProgress.coerceIn(0f, duration),
                onValueChange = { 
                    isScrubbing = true
                    scrubPosition = it 
                },
                onValueChangeFinished = {
                    isScrubbing = false
                    onSeek(scrubPosition.toLong())
                },
                valueRange = 0f..duration,
                modifier = Modifier.fillMaxWidth(),
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        drawStopIndicator = {
                            chapters.forEach { chapter ->
                                val fraction = chapter.startPosition.toFloat() / duration
                                if (fraction in 0.01f..0.99f) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx(),
                                        center = Offset(size.width * fraction, size.height / 2)
                                    )
                                }
                            }
                        }
                    )
                }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentProgress.toLong()), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(playbackState.duration), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls
        PlaybackControls(
            isPlaying = playbackState.isPlaying,
            onPlay = onPlay,
            onPause = onPause,
            onSeekBack = { onSeek((playbackState.currentPosition - 30000).coerceAtLeast(0)) },
            onSeekForward = { onSeek((playbackState.currentPosition + 30000).coerceAtMost(playbackState.duration)) },
            skippableSegment = skippableSegment,
            currentPosition = playbackState.currentPosition,
            onSkipSegment = { if (skippableSegment != null) onSeek(skippableSegment.last) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Volume Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (volume > 0f) onSetVolume(0f) else onSetVolume(0.5f) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    painter = painterResource(if (volume == 0f) CoreR.drawable.ic_volume_0 else CoreR.drawable.ic_volume),
                    contentDescription = if (volume == 0f) "Unmute" else "Mute"
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            VolumeSlider(
                volume = volume,
                onValueChange = onSetVolume,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp)) // Added bottom padding
    }

    if (showTrackSelection) {
        CastTrackSelectionDialog(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            onSetAudioTrack = onSetAudioTrack,
            onSetSubtitleTrack = { onSetSubtitleTrack(it) },
            onDismiss = { showTrackSelection = false }
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    skippableSegment: LongRange?,
    currentPosition: Long,
    onSkipSegment: () -> Unit
) {
    val isSkippable = skippableSegment != null && currentPosition in skippableSegment

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    ) {
        val backInteractionSource = remember { MutableInteractionSource() }
        val nextInteractionSource = remember { MutableInteractionSource() }
        val playPauseInteractionSource = remember { MutableInteractionSource() }

        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
        val isBackPressed by backInteractionSource.collectIsPressedAsState()
        val isNextPressed by nextInteractionSource.collectIsPressedAsState()

        val playPauseWeight by animateFloatAsState(
            targetValue = if (isPlayPausePressed) 1.9f else if (isBackPressed || (isNextPressed && !isSkippable)) 1.1f else 1.3f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "playPauseWeight",
        )

        val backButtonWeight by animateFloatAsState(
            targetValue = if (isBackPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "backButtonWeight",
        )

        val nextButtonWeight by animateFloatAsState(
            targetValue = if (isSkippable) 1.2f else if (isNextPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "nextButtonWeight",
        )

        FilledIconButton(
            onClick = onSeekBack,
            shape = RoundedCornerShape(50),
            interactionSource = backInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.height(68.dp).weight(backButtonWeight),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_skip_back),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = { if (isPlaying) onPause() else onPlay() },
            shape = RoundedCornerShape(50),
            interactionSource = playPauseInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.height(68.dp).weight(playPauseWeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) CoreR.drawable.ic_pause else CoreR.drawable.ic_play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPlaying) "Pause" else "Play",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = { if (isSkippable) onSkipSegment() else onSeekForward() },
            shape = RoundedCornerShape(50),
            interactionSource = nextInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isSkippable) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSkippable) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.height(68.dp).weight(nextButtonWeight).animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_skip_forward),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                if (isSkippable) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Skip Segment", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

private object VolumeSliderDefaults {
    val TrackHeight: Dp = 40.dp
    val HandleHeight: Dp = 52.dp
    val HandleWidth: Dp = 4.dp
    val TrackCornerRadius: Dp = 12.dp
    val InsetIconSize: Dp = 24.dp
    val IconPadding: Dp = 10.dp
    val ThumbTrackGapSize: Dp = 6.dp
    val StopIndicatorRadius: Dp = 4.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSlider(
    volume: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIcon = when {
        volume <= 0f -> painterResource(CoreR.drawable.ic_volume_0)
        volume < 0.66f -> painterResource(CoreR.drawable.ic_volume_50)
        else -> painterResource(CoreR.drawable.ic_volume)
    }
    
    val colors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        thumbColor = MaterialTheme.colorScheme.primary
    )

    Slider(
        value = volume,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = 0f..1f,
        colors = colors,
        track = { sliderState ->
            val iconSize = DpSize(VolumeSliderDefaults.InsetIconSize, VolumeSliderDefaults.InsetIconSize)
            val activeIconColor = MaterialTheme.colorScheme.onPrimary
            val inactiveIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            val volumeOffIcon = painterResource(CoreR.drawable.ic_volume_0)

            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier
                    .height(VolumeSliderDefaults.TrackHeight)
                    .drawWithContent {
                        drawContent()
                        val yOffset = size.height / 2 - iconSize.toSize().height / 2
                        val fraction = volume.coerceIn(0f, 1f)
                        val thumbGapPx = VolumeSliderDefaults.ThumbTrackGapSize.toPx()
                        val activeTrackEnd = size.width * fraction - thumbGapPx
                        val inactiveTrackStart = activeTrackEnd + thumbGapPx * 2
                        val inactiveTrackWidth = size.width - inactiveTrackStart

                        drawVolumeIcon(
                            icon = currentIcon,
                            iconSize = iconSize,
                            iconPadding = VolumeSliderDefaults.IconPadding,
                            yOffset = yOffset,
                            activeTrackWidth = activeTrackEnd,
                            inactiveTrackStart = inactiveTrackStart,
                            inactiveTrackWidth = inactiveTrackWidth,
                            activeIconColor = activeIconColor,
                            inactiveIconColor = inactiveIconColor,
                            volumeOffIcon = volumeOffIcon
                        )
                    },
                colors = colors,
                enabled = true,
                thumbTrackGapSize = VolumeSliderDefaults.ThumbTrackGapSize,
                trackInsideCornerSize = VolumeSliderDefaults.TrackCornerRadius,
                drawStopIndicator = null
            )
        }
    )
}

private fun DrawScope.drawVolumeIcon(
    icon: Painter,
    iconSize: DpSize,
    iconPadding: Dp,
    yOffset: Float,
    activeTrackWidth: Float,
    inactiveTrackStart: Float,
    inactiveTrackWidth: Float,
    activeIconColor: Color,
    inactiveIconColor: Color,
    volumeOffIcon: Painter
) {
    val iconSizePx = iconSize.toSize()
    val iconPaddingPx = iconPadding.toPx()
    val minSpaceForIcon = iconSizePx.width + iconPaddingPx * 2

    if (activeTrackWidth >= minSpaceForIcon) {
        translate(iconPaddingPx, yOffset) {
            with(icon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(activeIconColor))
            }
        }
    } else if (inactiveTrackWidth >= minSpaceForIcon) {
        translate(inactiveTrackStart + iconPaddingPx, yOffset) {
            with(volumeOffIcon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(inactiveIconColor))
            }
        }
    }
}

@Composable
fun CastTrackSelectionDialog(
    castManager: CastManager,
    onDismiss: () -> Unit
) {
    val audioTracks by castManager.audioTracks.collectAsState()
    val subtitleTracks by castManager.subtitleTracks.collectAsState()

    CastTrackSelectionDialog(
        audioTracks = audioTracks,
        subtitleTracks = subtitleTracks,
        onSetAudioTrack = { castManager.setAudioTrack(it) },
        onSetSubtitleTrack = { castManager.setSubtitleTrack(it) },
        onDismiss = onDismiss
    )
}

@Composable
fun CastTrackSelectionDialog(
    audioTracks: List<Track>,
    subtitleTracks: List<Track>,
    onSetAudioTrack: (Track?) -> Unit,
    onSetSubtitleTrack: (Track?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tracks") },
        text = {
            LazyColumn {
                item {
                    Text("Audio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                item {
                    TrackRow(trackName = "None (Audio)", isSelected = audioTracks.none { it.selected }) {
                        onSetAudioTrack(null)
                    }
                }
                items(audioTracks) { track ->
                    TrackRow(trackName = track.label ?: track.language ?: "Unknown", isSelected = track.selected) {
                        onSetAudioTrack(track)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Subtitles", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                item {
                    TrackRow(trackName = "None (Subtitle)", isSelected = subtitleTracks.none { it.selected }) {
                        onSetSubtitleTrack(null)
                    }
                }
                items(subtitleTracks) { track ->
                    TrackRow(trackName = track.label ?: track.language ?: "Unknown", isSelected = track.selected) {
                        onSetSubtitleTrack(track)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TrackRow(trackName: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = trackName, style = MaterialTheme.typography.bodyMedium)
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
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun CastExpandedPlayerContentPreview() {
    FindroidTheme {
        CastExpandedPlayerContentLayout(
            playbackState = CastPlaybackState(
                isPlaying = true,
                currentPosition = 300000L,
                duration = 1200000L
            ),
            connectedDeviceName = "Living Room TV",
            volume = 0.5f,
            title = "Big Buck Bunny",
            mediaType = PlayerMediaType.MOVIE,
            trickplayWidth = null,
            trickplayHeight = null,
            chapters = emptyList(),
            audioTracks = listOf(
                Track(0, "English (Stereo)", "en", "aac", true, true),
                Track(1, "French", "fr", "ac3", false, true)
            ),
            subtitleTracks = listOf(
                Track(2, "English", "en", "srt", false, true),
                Track(3, "Spanish", "es", "vtt", false, true)
            ),
            posterUrl = null,
            trickplay = null,
            skippableSegment = 100000L..200000L,
            onPlay = {},
            onPause = {},
            onSeek = {},
            onSetVolume = {},
            onSetAudioTrack = {},
            onSetSubtitleTrack = {},
            onDeviceClick = {},
            onClose = {}
        )
    }
}
