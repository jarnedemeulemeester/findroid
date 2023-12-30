package dev.jdtech.jellyfin.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.EmptyResultBackNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import dev.jdtech.jellyfin.models.Track
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination(style = BaseDialogStyle::class)
@Composable
fun VideoPlayerTrackSelectorDialog(
    tracks: ArrayList<Track>,
    resultNavigator: ResultBackNavigator<Int>,
) {
    Surface {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
        ) {
            Text(
                text = "Select track",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium - MaterialTheme.spacings.extraSmall),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacings.extraSmall),
            ) {
                items(tracks) { track ->
                    Surface(
                        onClick = {
                            resultNavigator.navigateBack(result = track.id)
                        },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
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
                            Text(text = listOf(track.label, track.language, track.codec).mapNotNull { it }.joinToString(" - "), style = MaterialTheme.typography.bodyLarge)
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
            tracks = arrayListOf(
                Track(
                    id = 0,
                    label = null,
                    language = "English",
                    codec = "flac",
                    selected = true,
                ),
                Track(
                    id = 0,
                    label = null,
                    language = "Japanese",
                    codec = "flac",
                    selected = false,
                ),
            ),
            resultNavigator = EmptyResultBackNavigator(),
        )
    }
}
