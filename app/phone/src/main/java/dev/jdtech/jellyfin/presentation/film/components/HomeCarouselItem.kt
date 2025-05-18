package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun HomeCarouselItem(
    item: FindroidItem,
    onAction: (HomeAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.77f)
            .clickable {
                onAction(HomeAction.OnItemClick(item))
            }
            .clip(MaterialTheme.shapes.large),
    ) {
        AsyncImage(
            model = item.images.backdrop,
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
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                    startY = size.height / 2,
                ),
            )
        }
        Text(
            text = item.name,
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.spacings.default,
                    vertical = MaterialTheme.spacings.medium,
                )
                .align(Alignment.BottomStart),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
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
