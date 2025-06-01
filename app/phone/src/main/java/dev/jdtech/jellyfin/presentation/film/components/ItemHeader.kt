package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.parallaxLayoutModifier

@Composable
fun ItemHeader(
    item: FindroidItem,
    scrollState: ScrollState,
    showLogo: Boolean = false,
    content: @Composable (BoxScope.() -> Unit) = {},
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    val backdropUri = when (item) {
        is FindroidEpisode -> item.images.primary
        else -> item.images.backdrop
    }

    val logoUri = when (item) {
        is FindroidEpisode -> item.images.showLogo
        else -> item.images.logo
    }

    Box(
        modifier = Modifier
            .height(240.dp)
            .clipToBounds(),
    ) {
        AsyncImage(
            model = backdropUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .parallaxLayoutModifier(
                    scrollState = scrollState,
                    rate = 2,
                ),
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
            contentScale = ContentScale.Crop,
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            drawRect(
                Color.Black.copy(alpha = 0.1f),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, backgroundColor),
                    startY = 0f,
                ),
            )
        }
        content()
        if (showLogo) {
            AsyncImage(
                model = logoUri,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(MaterialTheme.spacings.default)
                    .height(100.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
