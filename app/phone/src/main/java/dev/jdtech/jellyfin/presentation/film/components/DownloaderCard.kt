package dev.jdtech.jellyfin.presentation.film.components

import android.app.DownloadManager
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlin.math.roundToInt

@Composable
fun DownloaderCard(state: DownloaderState, onCancelClick: () -> Unit, onRetryClick: () -> Unit) {
    // Null means the total size is unknown, which is always so for a transcode: the server makes
    // it on the fly and sends no Content-Length. There is no percentage to draw, so the bar goes
    // indeterminate and the byte count stands in for it.
    val progress = state.progress
    val animatedProgress by
        animateFloatAsState(
            targetValue = progress ?: 0f,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        )
    val downloadedText =
        if (progress == null && state.bytesDownloaded > 0) {
            Formatter.formatShortFileSize(LocalContext.current, state.bytesDownloaded)
        } else {
            null
        }

    val textColor =
        when (state.status) {
            DownloadManager.STATUS_PAUSED -> Color.Yellow
            DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }

    val statusText =
        when (state.status) {
            DownloadManager.STATUS_PENDING -> stringResource(CoreR.string.download_pending)
            DownloadManager.STATUS_PAUSED -> stringResource(CoreR.string.download_paused)
            DownloadManager.STATUS_FAILED -> stringResource(CoreR.string.download_failed)
            else -> stringResource(CoreR.string.download_downloading)
        }

    val progressIndicatorColor =
        when (state.status) {
            DownloadManager.STATUS_PAUSED -> Color.Yellow
            DownloadManager.STATUS_SUCCESSFUL -> Color.Green
            DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
            else -> ProgressIndicatorDefaults.linearColor
        }

    val progressTrackColor =
        when (state.status) {
            DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.errorContainer
            else -> ProgressIndicatorDefaults.linearTrackColor
        }

    OutlinedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Local vals: DownloaderState lives in another module, so its nullable properties
                // cannot be smart cast here.
                val itemsTotal = state.itemsTotal
                val itemsCompleted = state.itemsCompleted
                val counter =
                    if (itemsTotal != null && itemsCompleted != null) "$itemsCompleted/$itemsTotal"
                    else null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                        Text(
                            text = statusText,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (counter != null) {
                            Text(
                                text = counter,
                                modifier = Modifier.alpha(0.7f),
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    when {
                        progress != null ->
                            Text(
                                text = animatedProgress.times(100).roundToInt().toString() + "%",
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        downloadedText != null ->
                            Text(
                                text = downloadedText,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                when {
                    // A bar pinned at zero reads as a stalled download, so anything still moving
                    // without a known total gets the indeterminate one instead. A paused download
                    // keeps the static bar: it is not moving, and the label already says so.
                    state.status == DownloadManager.STATUS_PENDING ||
                        (progress == null && state.status == DownloadManager.STATUS_RUNNING) -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    else -> {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = progressIndicatorColor,
                            trackColor = progressTrackColor,
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                if (state.errorText != null) {
                    Text(
                        text = state.errorText!!.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                when (state.status) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    // A paused download holds up the whole sequential queue, so it has to stay
                    // cancellable — the item buttons row is hidden while the card is mounted.
                    DownloadManager.STATUS_PAUSED -> {
                        FilledTonalIconButton(onClick = onCancelClick) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_x),
                                contentDescription = null,
                            )
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        FilledTonalIconButton(onClick = onRetryClick) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun DownloaderCardPendingPreview() {
    FindroidTheme {
        DownloaderCard(
            state = DownloaderState(status = DownloadManager.STATUS_PENDING),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview
private fun DownloaderCardDownloadingPreview() {
    FindroidTheme {
        DownloaderCard(
            state = DownloaderState(status = DownloadManager.STATUS_RUNNING, progress = 0.5f),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview
private fun DownloaderCardBatchPreview() {
    FindroidTheme {
        DownloaderCard(
            state =
                DownloaderState(
                    status = DownloadManager.STATUS_RUNNING,
                    progress = 0.84f,
                    itemsCompleted = 3,
                    itemsTotal = 13,
                ),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview
private fun DownloaderCardUnknownSizePreview() {
    FindroidTheme {
        DownloaderCard(
            state =
                DownloaderState(
                    status = DownloadManager.STATUS_RUNNING,
                    progress = null,
                    bytesDownloaded = 533_725_184,
                    itemsCompleted = 1,
                    itemsTotal = 10,
                ),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview
private fun DownloaderCardFailedPreview() {
    FindroidTheme {
        DownloaderCard(
            state =
                DownloaderState(
                    status = DownloadManager.STATUS_FAILED,
                    progress = 0.5f,
                    errorText = UiText.DynamicString("Not enough storage space"),
                ),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}
