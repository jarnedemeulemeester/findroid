package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ItemCountBadge(
    unplayedItemCount: Int,
    modifier: Modifier = Modifier,
) {
    BaseBadge(
        modifier = modifier,
    ) {
        Text(
            text = unplayedItemCount.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = MaterialTheme.spacings.extraSmall),
        )
    }
}

@Composable
@Preview
private fun ItemCountBadgePreview() {
    FindroidTheme {
        ItemCountBadge(
            110,
        )
    }
}
