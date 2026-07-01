package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.core.presentation.utils.toBlurHashPainter
import dev.jdtech.jellyfin.core.presentation.utils.toOptimizedImageUri
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun HomeCarouselItem(item: FindroidItem, onAction: (HomeAction) -> Unit) {
    val colorStops =
        arrayOf(
            0.0f to Color.Black.copy(alpha = 0.1f),
            0.5f to Color.Black.copy(alpha = 0.5f),
            1f to Color.Black.copy(alpha = 0.6f),
        )
    
    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.large)
            .clickable {
                onAction(HomeAction.OnItemClick(item))
            }
    ) {
        val image = item.images.backdrop

        val imageUri = image?.uri.toOptimizedImageUri(widthDp = maxWidth, heightDp = maxHeight)

        val blurPlaceholder = remember(image?.blurHash) {
            image?.blurHash.toBlurHashPainter()
        }

        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = blurPlaceholder ?: ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(colorStops = colorStops))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            modifier =
                Modifier
                    .padding(
                        horizontal = MaterialTheme.spacings.default,
                        vertical = MaterialTheme.spacings.medium,
                    )
                    .align(Alignment.BottomStart)
                    .onGloballyPositioned { coordinates -> coordinates.size },
        ) {
            val genres =
                when (item) {
                    is FindroidMovie -> item.genres
                    is FindroidShow -> item.genres
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
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeCarouselItemPreview() {
    FindroidTheme { HomeCarouselItem(item = dummyMovie, onAction = {}) }
}
