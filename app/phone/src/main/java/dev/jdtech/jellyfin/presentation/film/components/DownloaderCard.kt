package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlin.math.roundToInt

@Composable
fun DownloaderCard(
    state: DownloaderState,
) {
    val animatedProgress by
        animateFloatAsState(
            targetValue = state.progress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        )

    Card {
        Column(
            modifier = Modifier
                .padding(MaterialTheme.spacings.medium),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Downloading...", style = MaterialTheme.typography.bodyLarge)
                Text(animatedProgress.times(100).roundToInt().toString() + "%", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(MaterialTheme.spacings.small))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
@Preview
private fun DownloaderCardPreview() {
    FindroidTheme {
        DownloaderCard(
            state = DownloaderState(
                progress = 0.5f,
            ),
        )
    }
}
