package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ActorsRow(
    actors: List<FindroidPerson>,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .padding(contentPadding),
    ) {
        Text(
            text = stringResource(CoreR.string.cast_amp_crew),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
    }
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        items(
            items = actors,
            key = { person ->
                person.id
            },
        ) { person ->
            PersonItem(
                person = person,
            )
        }
    }
}
