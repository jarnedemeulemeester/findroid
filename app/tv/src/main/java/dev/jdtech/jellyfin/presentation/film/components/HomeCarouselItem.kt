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
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastShow
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun HomeCarouselItem(
    item: JellyCastItem,
    onAction: (HomeAction) -> Unit,
) {
    val colorStops = arrayOf(
        0.0f to Color.Black.copy(alpha = 0.1f),
        0.5f to Color.Black.copy(alpha = 0.5f),
        1f to Color.Black.copy(alpha = 0.6f),
    )

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
                        colorStops = colorStops,
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
                val genres = when (item) {
                    is JellyCastMovie -> item.genres
                    is JellyCastShow -> item.genres
                    else -> emptyList()
                }
                Text(
                    text = genres.joinToString(),
                    color = Color.LightGray,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = item.name,
                    modifier = Modifier,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
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
    JellyCastTheme {
        HomeCarouselItem(
            item = dummyMovie,
            onAction = {},
        )
    }
}
