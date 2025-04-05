package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun ProgressBar(
    item: FindroidItem,
    width: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(
                    item.playbackPositionTicks
                        .div(
                            item.runtimeTicks.toFloat(),
                        )
                        .times(
                            width - 16,
                        ).dp,
                )
                .clip(
                    MaterialTheme.shapes.extraSmall,
                )
                .background(
                    MaterialTheme.colorScheme.primary,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressBarPreview() {
    FindroidTheme {
        ProgressBar(
            item = dummyMovie,
            width = 142,
        )
    }
}
