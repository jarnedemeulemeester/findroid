package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerControlsLayout(
    mediaTitle: @Composable () -> Unit,
    seeker: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        mediaTitle()
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
        )
    }
}
