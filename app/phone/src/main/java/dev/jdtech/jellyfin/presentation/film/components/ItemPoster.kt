package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.utils.toBlurHash
import dev.jdtech.jellyfin.presentation.utils.toLocalFilesUri
import dev.jdtech.jellyfin.presentation.utils.withJellyfinResize

enum class Direction {
    HORIZONTAL,
    VERTICAL,
}

@Composable
fun ItemPoster(
    item: FindroidItem,
    direction: Direction,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val image = when (direction) {
        Direction.HORIZONTAL -> {
            item.images.backdrop ?: item.images.primary
        }
        Direction.VERTICAL -> {
            if (item is FindroidEpisode) item.images.showPrimary ?: item.images.primary
            else item.images.primary ?: item.images.backdrop
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .aspectRatio(if (direction == Direction.HORIZONTAL) 16f / 9f else 2f / 3f)
                .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val imageUri =
            image?.uri
                .withJellyfinResize(widthDp = maxWidth, heightDp = maxHeight)
                .toLocalFilesUri(context)

        val blurPlaceholder = remember(image?.blurHash) {
            image?.blurHash.toBlurHash()
        }

        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = blurPlaceholder,
            error = blurPlaceholder,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
