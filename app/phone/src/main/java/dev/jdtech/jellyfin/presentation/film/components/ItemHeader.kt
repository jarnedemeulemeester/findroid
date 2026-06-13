package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.parallaxLayoutModifier
import dev.jdtech.jellyfin.presentation.utils.toBlurHash
import dev.jdtech.jellyfin.presentation.utils.toLocalFilesUri
import dev.jdtech.jellyfin.presentation.utils.withJellyfinResize

@Composable
fun ItemHeader(
    item: FindroidItem,
    scrollState: ScrollState,
    showLogo: Boolean = false,
    content: @Composable (BoxScope.() -> Unit) = {},
) {
    ItemHeaderBase(
        item = item,
        showLogo = showLogo,
        backdropImage = {
            val context = LocalContext.current
            val image = when (item) {
                is FindroidEpisode -> item.images.primary
                else -> item.images.backdrop
            }

            val backdropUri = image?.uri
                .withJellyfinResize(widthDp = maxWidth, heightDp = maxHeight)
                .toLocalFilesUri(context)

            val blurPlaceholder = remember(image?.blurHash) {
                image?.blurHash.toBlurHash()
            }

            AsyncImage(
                model = backdropUri,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .parallaxLayoutModifier(scrollState = scrollState, rate = 2),
                placeholder = blurPlaceholder ?: ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
                contentScale = ContentScale.Crop,
            )
        },
        content = content,
    )
}

@Composable
fun ItemHeader(
    item: FindroidItem,
    lazyListState: LazyListState,
    showLogo: Boolean = false,
    content: @Composable (BoxScope.() -> Unit) = {},
) {
    ItemHeaderBase(
        item = item,
        showLogo = showLogo,
        backdropImage = {
            val context = LocalContext.current
            val image = when (item) {
                is FindroidEpisode -> item.images.primary
                is FindroidSeason -> item.images.showBackdrop
                else -> item.images.backdrop
            }

            val backdropUri = image?.uri
                .withJellyfinResize(widthDp = maxWidth, heightDp = maxHeight)
                .toLocalFilesUri(context)

            val blurPlaceholder = remember(image?.blurHash) {
                image?.blurHash.toBlurHash()
            }

            AsyncImage(
                model = backdropUri,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .parallaxLayoutModifier(lazyListState = lazyListState, rate = 2),
                placeholder = blurPlaceholder ?: ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
                contentScale = ContentScale.Crop,
            )
        },
        content = content,
    )
}

@Composable
private fun ItemHeaderBase(
    item: FindroidItem,
    showLogo: Boolean = false,
    backdropImage: @Composable (BoxWithConstraintsScope.() -> Unit),
    content: @Composable (BoxScope.() -> Unit) = {},
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    val logo =
        when (item) {
            is FindroidEpisode -> item.images.showLogo
            else -> item.images.logo
        }

    BoxWithConstraints(modifier = Modifier
        .height(288.dp)
        .clipToBounds()) {
        backdropImage()
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.1f))
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, backgroundColor),
                        startY = 0f,
                    )
            )
        }
        content()
        if (showLogo) {
            AsyncImage(
                model = logo?.uri,
                contentDescription = null,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(MaterialTheme.spacings.default)
                        .height(100.dp)
                        .fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
