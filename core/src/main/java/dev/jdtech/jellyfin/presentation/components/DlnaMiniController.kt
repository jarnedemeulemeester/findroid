package dev.jdtech.jellyfin.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.dlna.DlnaHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mini controller for DLNA playback
 * Shows at the bottom of the screen when DLNA is active
 * Can be expanded to show full controls
 */
@Composable
fun DlnaMiniController(
    modifier: Modifier = Modifier,
    isOnHomePage: Boolean = false,
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentTime by remember { mutableStateOf("00:00") }
    var duration by remember { mutableStateOf("00:00") }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // Update state periodically
    DisposableEffect(Unit) {
        var updateJob: Job? = null
        
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val isDlnaActive = DlnaHelper.isDlnaDeviceAvailable(context)
                isVisible = isDlnaActive
                
                if (isDlnaActive) {
                    DlnaHelper.getCurrentPosition(context) { position ->
                        currentPosition = position
                    }
                    totalDuration = DlnaHelper.getDuration(context)
                    
                    if (totalDuration > 0 && !isSeeking) {
                        progress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                        currentTime = formatTime(currentPosition)
                        duration = formatTime(totalDuration)
                    }
                    
                    // Get playing state from DlnaHelper
                    isPlaying = DlnaHelper.isPlaying()
                    
                    // Get media info
                    title = DlnaHelper.getMediaTitle(context)
                    subtitle = DlnaHelper.getMediaSubtitle(context)
                    imageUrl = DlnaHelper.getMediaImageUrl(context)
                    
                    DlnaHelper.getVolume(context) { vol ->
                        volume = vol.toFloat()
                    }
                } else {
                    // Reset playing state when DLNA is not active
                    isPlaying = false
                }
                
                delay(1000) // Update every second
            }
        }
        
        onDispose {
            updateJob?.cancel()
        }
    }
    
    val playerHeight by animateDpAsState(
        targetValue = if (isExpanded) 500.dp else 80.dp,
        label = "playerHeight"
    )
    
    // When collapsed and on home page, add bottom padding to stay above navbar (80dp)
    val bottomPadding by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else if (isOnHomePage) 80.dp else 0.dp,
        label = "bottomPadding"
    )
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.padding(bottom = bottomPadding)
    ) {
        Surface(
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(playerHeight)
        ) {
            if (isExpanded) {
                // Expanded player
                ExpandedDlnaPlayer(
                    title = title,
                    subtitle = subtitle,
                    imageUrl = imageUrl,
                    progress = progress,
                    currentTime = currentTime,
                    duration = duration,
                    volume = volume,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration,
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) {
                            DlnaHelper.pause(context)
                        } else {
                            DlnaHelper.play(context)
                        }
                    },
                    onSeek = { position ->
                        isSeeking = false
                        DlnaHelper.seek(context, position)
                    },
                    onSeekStart = { isSeeking = true },
                    onVolumeChange = { newVolume ->
                        DlnaHelper.setVolume(context, newVolume.toDouble())
                    },
                    onRewind = {
                        DlnaHelper.seek(context, (currentPosition - 10000).coerceAtLeast(0))
                    },
                    onForward = {
                        DlnaHelper.seek(context, (currentPosition + 10000).coerceAtMost(totalDuration))
                    },
                    onClose = {
                        DlnaHelper.stopDlna(context)
                    },
                    onCollapse = {
                        isExpanded = false
                    }
                )
            } else {
                // Mini player
                MiniDlnaPlayer(
                    title = title,
                    currentTime = currentTime,
                    duration = duration,
                    progress = progress,
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) {
                            DlnaHelper.pause(context)
                        } else {
                            DlnaHelper.play(context)
                        }
                    },
                    onRewind = {
                        DlnaHelper.seek(context, (currentPosition - 10000).coerceAtLeast(0))
                    },
                    onForward = {
                        DlnaHelper.seek(context, (currentPosition + 10000).coerceAtMost(totalDuration))
                    },
                    onClose = {
                        DlnaHelper.stopDlna(context)
                    },
                    onExpand = {
                        isExpanded = true
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniDlnaPlayer(
    title: String,
    currentTime: String,
    duration: String,
    progress: Float,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onExpand() }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title and time
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$currentTime / $duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind
                IconButton(
                    onClick = onRewind,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rewind),
                        contentDescription = "Rewind"
                    )
                }
                
                // Play/Pause toggle button
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Forward
                IconButton(
                    onClick = onForward,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_forward),
                        contentDescription = "Fast forward"
                    )
                }
                
                // Stop DLNA
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = "Stop DLNA"
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedDlnaPlayer(
    title: String,
    subtitle: String,
    imageUrl: String?,
    progress: Float,
    currentTime: String,
    duration: String,
    volume: Float,
    currentPosition: Long,
    totalDuration: Long,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onClose: () -> Unit,
    onCollapse: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = "Collapse"
                )
            }
            
            Text(
                text = "Reproduciendo en DLNA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "Stop DLNA"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Album art / Poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_film),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title and subtitle
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Progress slider
        var sliderPosition by remember { mutableFloatStateOf(progress) }
        
        Column {
            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    onSeekStart()
                    sliderPosition = newValue
                },
                onValueChangeFinished = {
                    val seekPosition = (sliderPosition * totalDuration).toLong()
                    onSeek(seekPosition)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime((sliderPosition * totalDuration).toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rewind
            IconButton(
                onClick = onRewind,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rewind),
                    contentDescription = "Rewind 10s",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Play/Pause toggle button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Forward
            IconButton(
                onClick = onForward,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fast_forward),
                    contentDescription = "Forward 10s",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Volume control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume),
                contentDescription = "Volume",
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
