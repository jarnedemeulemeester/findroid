package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard

@Composable
fun HomeSection(
    section: HomeSection,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = section.name.asString(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(itemsPadding),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            contentPadding = itemsPadding,
        ) {
            items(section.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.HORIZONTAL,
                    onClick = {
                        onAction(HomeAction.OnItemClick(it))
                    },
                )
            }
        }
    }
}
