package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun CastTrackSelectionDialog(
    audioTracks: List<Track>,
    subtitleTracks: List<Track>,
    onSetAudioTrack: (Track?) -> Unit,
    onSetSubtitleTrack: (Track?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .padding(MaterialTheme.spacings.medium)
                    .widthIn(max = 400.dp)
                    .heightIn(max = 540.dp)
                    .fillMaxWidth(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks on surface */ }
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacings.medium)
                ) {
                    // Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))

                    Text(
                        text = "Select Tracks",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                            Text(
                                text = "Subtitles",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            )
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

                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CastTrackSelectionDialogPreview() {
    FindroidTheme {
        CastTrackSelectionDialog(
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
