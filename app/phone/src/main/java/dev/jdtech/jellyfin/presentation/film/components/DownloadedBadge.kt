package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun DownloadedBadge(
    modifier: Modifier = Modifier,
) {
    BaseBadge(
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(CoreR.drawable.ic_download),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center),
        )
    }
}

@Composable
@Preview
private fun DownloadedBadgePreview() {
    FindroidTheme {
        DownloadedBadge()
    }
}
