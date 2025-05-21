package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun HomeCarouselItem(
    item: FindroidItem,
    onAction: (HomeAction) -> Unit,
) {
    Surface(
        onClick = { onAction(HomeAction.OnItemClick(item)) },
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(
                    4.dp,
                    Color.White,
                ),
                shape = MaterialTheme.shapes.large,
            ),
        ),
        scale = ClickableSurfaceScale.None,
    ) {
        Box {
            AsyncImage(
                model = item.images.backdrop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surface),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(),
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.6f)),
                    ),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.spacings.default,
                        vertical = MaterialTheme.spacings.default,
                    )
                    .align(Alignment.BottomStart),
            ) {
                when (item) {
                    is FindroidMovie -> {
                        Text(
                            text = item.genres.joinToString(),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    is FindroidShow -> {
                        Text(
                            text = item.genres.joinToString(),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Text(
                    text = item.name,
                    modifier = Modifier,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = item.overview,
                    modifier = Modifier
                        .width(640.dp),
                    color = Color.LightGray,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeCarouselItemPreview() {
    FindroidTheme {
        HomeCarouselItem(
            item = dummyMovie,
            onAction = {},
        )
    }
}
