package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun VideoPlayerControlsLayout(
    mediaTitle: @Composable () -> Unit,
    seeker: @Composable () -> Unit,
    mediaActions: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                mediaTitle()
            }
            mediaActions()
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
        seeker()
    }
}

@Preview
@Composable
private fun VideoPlayerControlsLayoutPreview() {
    FindroidTheme {
        VideoPlayerControlsLayout(
            mediaTitle = {
                Box(
                    Modifier
                        .border(2.dp, Color.Red)
                        .background(Color.LightGray)
                        .fillMaxWidth()
                        .height(96.dp),
                )
            },
            seeker = {
                Box(
                    Modifier
                        .border(2.dp, Color.Red)
                        .background(Color.LightGray)
                        .fillMaxWidth()
                        .height(48.dp),
                )
            },
            mediaActions = {
                Box(
                    Modifier
                        .border(2.dp, Color.Red)
                        .background(Color.LightGray)
                        .fillMaxWidth()
                        .height(48.dp),
                )
            },
        )
    }
}
