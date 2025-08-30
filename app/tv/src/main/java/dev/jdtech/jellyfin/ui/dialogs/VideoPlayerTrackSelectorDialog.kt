package dev.jdtech.jellyfin.ui.dialogs

import android.os.Parcelable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.models.Track
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlinx.parcelize.Parcelize
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.player.video.R as PlayerVideoR

@Parcelize
data class VideoPlayerTrackSelectorDialogResult(
    val trackType: @C.TrackType Int,
    val index: Int,
) : Parcelable

@Composable
fun VideoPlayerTrackSelectorDialog(
    trackType: @C.TrackType Int,
    tracks: Array<Track>,
    // resultNavigator: ResultBackNavigator<VideoPlayerTrackSelectorDialogResult>,
) {
    val dialogTitle = when (trackType) {
        C.TRACK_TYPE_AUDIO -> PlayerVideoR.string.select_audio_track
        C.TRACK_TYPE_TEXT -> PlayerVideoR.string.select_subtitle_track
        else -> CoreR.string.unknown_error
    }
    Surface {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
        ) {
            Text(
                text = stringResource(id = dialogTitle),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium - MaterialTheme.spacings.extraSmall),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacings.extraSmall),
            ) {
                items(tracks) { track ->
                    Surface(
                        onClick = {
                            // resultNavigator.navigateBack(result = VideoPlayerTrackSelectorDialogResult(trackType, track.id))
                        },
                        enabled = track.supported,
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                BorderStroke(
                                    4.dp,
                                    Color.White,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            ),
                        ),
                        scale = ClickableSurfaceScale.None,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(MaterialTheme.spacings.extraSmall),
                        ) {
                            RadioButton(
                                selected = track.selected,
                                onClick = null,
                                enabled = true,
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
                            Text(
                                text = listOf(track.label, track.language, track.codec)
                                    .mapNotNull { it }
                                    .joinToString(" - ")
                                    .ifEmpty { stringResource(id = PlayerVideoR.string.none) },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun VideoPlayerTrackSelectorDialogPreview() {
    FindroidTheme {
        VideoPlayerTrackSelectorDialog(
            trackType = C.TRACK_TYPE_AUDIO,
            tracks = arrayOf(
                Track(
                    id = 0,
                    label = null,
                    language = "English",
                    codec = "flac",
                    selected = true,
                    supported = true,
                ),
                Track(
                    id = 0,
                    label = null,
                    language = "Japanese",
                    codec = "flac",
                    selected = false,
                    supported = true,
                ),
                Track(
                    id = 0,
                    label = null,
                    language = "English",
                    codec = "truehd",
                    selected = false,
                    supported = false,
                ),
            ),
        )
    }
}
