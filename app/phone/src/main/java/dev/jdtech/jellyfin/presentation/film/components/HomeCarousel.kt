package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCarousel(
    items: List<FindroidItem>,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
) {
    val itemWidth = 320.dp
    val carouselState = rememberCarouselState {
        items.size
    }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = itemWidth,
        itemSpacing = MaterialTheme.spacings.medium,
        contentPadding = itemsPadding,
    ) { index ->
        val item = items[index]

        var imageUri = item.images.backdrop

        val density = LocalDensity.current

        val opacity = remember(carouselItemInfo.size) {
            val minWidthPx = with(density) { 100.dp.toPx() }
            val maxWidthPx = with(density) { itemWidth.toPx() }
            val currentWidthPx = carouselItemInfo.size

            val clampedWidth = currentWidthPx.coerceIn(minWidthPx, maxWidthPx)

            (clampedWidth - minWidthPx) / (maxWidthPx - minWidthPx)
        }

        Box(
            modifier = Modifier
                .aspectRatio(1.77f)
                .maskClip(MaterialTheme.shapes.small)
                .clickable {
                    onAction(HomeAction.OnItemClick(item))
                },
        ) {
            AsyncImage(
                model = imageUri,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                        startY = size.height / 2,
                    ),
                )
            }
            Text(
                text = item.name,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacings.medium, vertical = MaterialTheme.spacings.small)
                    .align(Alignment.BottomStart)
                    .alpha(opacity),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeCarouselPreview() {
    FindroidTheme {
        HomeCarousel(
            items = dummyMovies,
            itemsPadding = PaddingValues(horizontal = 0.dp),
            onAction = {},
        )
    }
}
