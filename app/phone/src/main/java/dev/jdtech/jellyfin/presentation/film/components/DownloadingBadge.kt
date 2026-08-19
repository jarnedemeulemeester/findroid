package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

/**
 * Marks the episode the queue is currently working on. Indeterminate while the item is still
 * waiting its turn or starting, determinate once DownloadManager reports real progress.
 */
@Composable
fun DownloadingBadge(progress: Float?, modifier: Modifier = Modifier) {
    BaseBadge(modifier = modifier) {
        val indicatorModifier =
            Modifier.size(16.dp).align(Alignment.Center).padding(0.dp)
        if (progress == null) {
            CircularProgressIndicator(
                modifier = indicatorModifier,
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = indicatorModifier,
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
@Preview
private fun DownloadingBadgeIndeterminatePreview() {
    FindroidTheme { DownloadingBadge(progress = null) }
}

@Composable
@Preview
private fun DownloadingBadgeProgressPreview() {
    FindroidTheme { DownloadingBadge(progress = 0.42f) }
}
