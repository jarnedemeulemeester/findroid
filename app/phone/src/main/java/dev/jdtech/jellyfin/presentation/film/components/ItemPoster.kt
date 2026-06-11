package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.utils.LocalImageQuality
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
    episodeCard: Boolean = false,
) {
    val context = LocalContext.current
    val imageQuality = LocalImageQuality.current

    val imageUri = remember(item, direction, episodeCard, imageQuality) {
        val baseUri = when (direction) {
            Direction.HORIZONTAL -> {
                item.images.backdrop ?: item.images.primary
            }
            Direction.VERTICAL -> {
                if (item is FindroidEpisode) item.images.showPrimary ?: item.images.primary
                else item.images.primary ?: item.images.backdrop
            }
        }

        val (width, height) = if (episodeCard) {
            400 to 225
        } else {
            when (direction) {
                Direction.HORIZONTAL -> 640 to 360
                Direction.VERTICAL -> 400 to 600
            }
        }

        baseUri
            .withJellyfinResize(width, height, imageQuality = imageQuality)
            .toLocalFilesUri(context)
    }

    AsyncImage(
        model = imageUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(if (direction == Direction.HORIZONTAL) 16f / 9f else 2f / 3f)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}
