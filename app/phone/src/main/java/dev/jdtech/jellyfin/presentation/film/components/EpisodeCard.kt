package dev.jdtech.jellyfin.presentation.film.components

import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.utils.DownloadEntry
import dev.jdtech.jellyfin.utils.DownloadEntryStatus
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun EpisodeCard(
    episode: FindroidEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** This episode's place in the download queue, when it is in one. */
    downloadEntry: DownloadEntry? = null,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val activeEntry = downloadEntry?.takeIf { !it.status.isTerminal }
    val statusText =
        when (activeEntry?.status) {
            DownloadEntryStatus.QUEUED -> stringResource(CoreR.string.download_queued)
            DownloadEntryStatus.PAUSED -> stringResource(CoreR.string.download_paused)
            DownloadEntryStatus.PREPARING -> stringResource(CoreR.string.download_pending)
            DownloadEntryStatus.RUNNING ->
                when {
                    activeEntry.progress >= 0 ->
                        stringResource(CoreR.string.download_progress, activeEntry.progress)
                    // No total size to compute a percentage from, so report what has arrived.
                    activeEntry.bytesDownloaded > 0 ->
                        stringResource(
                            CoreR.string.download_downloading_size,
                            Formatter.formatShortFileSize(
                                LocalContext.current,
                                activeEntry.bytesDownloaded,
                            ),
                        )
                    else -> stringResource(CoreR.string.download_downloading)
                }
            else -> null
        }

    Row(
        modifier =
            modifier
                .height(84.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
    ) {
        Box {
            ItemPoster(
                item = episode,
                direction = Direction.HORIZONTAL,
                modifier = Modifier.clip(MaterialTheme.shapes.small),
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(MaterialTheme.spacings.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                if (activeEntry != null) {
                    DownloadingBadge(
                        progress =
                            activeEntry.progress
                                .takeIf {
                                    it >= 0 && activeEntry.status == DownloadEntryStatus.RUNNING
                                }
                                ?.div(100f)
                    )
                } else if (episode.isDownloaded()) DownloadedBadge()
                if (episode.played) PlayedBadge()
            }
        }
        Spacer(Modifier.width(MaterialTheme.spacings.default / 2))
        Box(modifier = Modifier.fillMaxHeight()) {
            Column {
                Text(
                    text =
                        stringResource(
                            id = dev.jdtech.jellyfin.core.R.string.episode_name,
                            episode.indexNumber,
                            episode.name,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (statusText != null) {
                    Text(
                        text = statusText,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = episode.overview,
                    modifier = Modifier.alpha(0.7f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Canvas(
                modifier =
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(MaterialTheme.spacings.default)
            ) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, backgroundColor),
                            startY = 0f,
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EpisodeCardPreview() {
    FindroidTheme { EpisodeCard(episode = dummyEpisode, onClick = {}) }
}
