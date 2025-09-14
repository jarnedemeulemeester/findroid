package dev.jdtech.jellyfin.presentation.film.components

import android.app.DownloadManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlin.math.roundToInt
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun DownloaderCard(
    state: DownloaderState,
    onCancelClick: () -> Unit,
) {
    val animatedProgress by
        animateFloatAsState(
            targetValue = state.progress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        )

    val progressIndicatorColor = when (state.status) {
        DownloadManager.STATUS_PAUSED -> Color.Yellow
        DownloadManager.STATUS_SUCCESSFUL -> Color.Green
        DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
        else -> ProgressIndicatorDefaults.linearColor
    }

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Downloading...",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = animatedProgress.times(100).roundToInt().toString() + "%",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                when (state.status) {
                    DownloadManager.STATUS_PENDING -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = progressIndicatorColor,
                        )
                    }
                }
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                FilledTonalIconButton(
                    onClick = onCancelClick,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_x),
                        contentDescription = null,
                    )
                }
            }
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
            onCancelClick = {},
        )
    }
}
