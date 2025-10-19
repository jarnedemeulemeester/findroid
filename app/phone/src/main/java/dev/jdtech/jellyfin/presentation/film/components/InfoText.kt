package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.models.JellyCastItemPerson
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun InfoText(
    genres: List<String>,
    director: JellyCastItemPerson?,
    writers: List<JellyCastItemPerson>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Text(
            text = "${stringResource(CoreR.string.genres)}: ${genres.joinToString()}",
            style = MaterialTheme.typography.bodyMedium,
        )
        director?.let { director ->
            Text(
                text = "${stringResource(CoreR.string.director)}: ${director.name}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (writers.isNotEmpty()) {
            Text(
                text = "${stringResource(CoreR.string.writers)}: ${writers.joinToString { it.name }}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
