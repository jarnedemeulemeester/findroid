package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import org.jellyfin.sdk.model.api.ImageType

enum class Direction {
    HORIZONTAL, VERTICAL
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ItemPoster(item: FindroidItem, baseUrl: String, direction: Direction) {
    var itemId = item.id
    var imageType = ImageType.PRIMARY

    if (direction == Direction.HORIZONTAL) {
        if (item is FindroidMovie) imageType = ImageType.BACKDROP
    } else {
        itemId = when (item) {
            is FindroidEpisode -> item.seriesId
            is FindroidSeason -> item.seriesId
            else -> item.id
        }
    }

    AsyncImage(
        model = "$baseUrl/items/$itemId/Images/$imageType",
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (direction == Direction.HORIZONTAL) 1.77f else 0.66f)
            .background(
                MaterialTheme.colorScheme.surface,
            ),
    )
}
