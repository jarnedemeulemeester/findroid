package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR

@Composable
fun HomeView(
    view: HomeItem.ViewItem,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(itemsPadding),
        ) {
            Text(
                text = stringResource(FilmR.string.latest_library, view.view.name),
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = {
                    onAction(
                        HomeAction.OnLibraryClick(
                            FindroidCollection(
                                id = view.view.id,
                                name = view.view.name,
                                images = FindroidImages(),
                                type = view.view.type,
                            ),
                        ),
                    )
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(stringResource(CoreR.string.view_all))
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
        LazyRow(
            contentPadding = itemsPadding,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
            items(view.view.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = {
                        onAction(HomeAction.OnItemClick(item))
                    },
                )
            }
        }
    }
}
