package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlin.math.roundToInt

@Composable
fun CastTrackSelectionSheet(
    audioTracks: List<Track>,
    subtitleTracks: List<Track>,
    onSetAudioTrack: (Track?) -> Unit,
    onSetSubtitleTrack: (Track?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetY, label = "offset")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedOffset.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val newOffset = offsetY + dragAmount
                        offsetY = newOffset.coerceAtLeast(0f)
                    }
                )
            },
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(MaterialTheme.spacings.medium)
                .heightIn(max = 450.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))

            Text(
                text = "Select Tracks",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                item {
                    Text(
                        text = "Audio",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                    )
                }
                item {
                    TrackRow(
                        trackName = "None (Audio)",
                        isSelected = audioTracks.none { it.selected }) {
                        onSetAudioTrack(null)
                    }
                }
                items(audioTracks) { track ->
                    TrackRow(
                        trackName = track.label ?: track.language ?: "Unknown",
                        isSelected = track.selected
                    ) {
                        onSetAudioTrack(track)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                    )
                }
                item {
                    TrackRow(
                        trackName = "None (Subtitle)",
                        isSelected = subtitleTracks.none { it.selected }) {
                        onSetSubtitleTrack(null)
                    }
                }
                items(subtitleTracks) { track ->
                    TrackRow(
                        trackName = track.label ?: track.language ?: "Unknown",
                        isSelected = track.selected
                    ) {
                        onSetSubtitleTrack(track)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CastTrackSelectionSheetPreview() {
    FindroidTheme {
        CastTrackSelectionSheet(
            audioTracks = listOf(
                Track(1, "English", "English", "ac3", true, true),
                Track(2, "Italian", "Italian", "aac", false, true)
            ),
            subtitleTracks = listOf(
                Track(3, "English", "English", "srt", false, true),
                Track(4, "Italian", "Italian", "srt", true, true)
            ),
            onSetAudioTrack = {},
            onSetSubtitleTrack = {},
            onDismiss = {}
        )
    }
}

@Composable
private fun TrackRow(trackName: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = trackName, style = MaterialTheme.typography.bodyMedium)
    }
}
