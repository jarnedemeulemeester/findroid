package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun InfoText(
    genres: List<String>,
    director: FindroidItemPerson?,
    writers: List<FindroidItemPerson>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        if (genres.isNotEmpty()) {
            Text(
                text = "${stringResource(CoreR.string.genres)}: ${genres.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (director != null) {
            Text(
                text = "${stringResource(CoreR.string.director)}: ${director.name}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (writers.isNotEmpty()) {
            Text(
                text =
                    "${stringResource(CoreR.string.writers)}: ${writers.joinToString { it.name }}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
