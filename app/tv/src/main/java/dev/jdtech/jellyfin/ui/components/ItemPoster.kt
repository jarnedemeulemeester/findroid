package dev.jdtech.jellyfin.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.presentation.utils.EpisodeThumbnailBlurTransformation
import dev.jdtech.jellyfin.presentation.utils.LocalBlurEpisodeThumbnails

enum class Direction {
    HORIZONTAL,
    VERTICAL,
}

@Composable
fun ItemPoster(item: FindroidItem, direction: Direction, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val blurEpisodeThumbnails = LocalBlurEpisodeThumbnails.current
    var imageUri = item.images.primary

    when (direction) {
        Direction.HORIZONTAL -> {
            if (item is FindroidMovie) imageUri = item.images.backdrop
        }
        Direction.VERTICAL -> {
            when (item) {
                is FindroidEpisode -> imageUri = item.images.showPrimary
            }
        }
    }

    val blurTransformation = remember(imageUri) { EpisodeThumbnailBlurTransformation(context) }

    val shouldBlur =
        blurEpisodeThumbnails &&
            direction == Direction.HORIZONTAL &&
            item is FindroidEpisode &&
            !item.played
    val imageModel =
        if (shouldBlur && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ImageRequest.Builder(context).data(imageUri).transformations(blurTransformation).build()
        } else {
            imageUri
        }

    AsyncImage(
        model = imageModel,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(if (direction == Direction.HORIZONTAL) 1.77f else 0.66f)
                .then(
                    if (shouldBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(30.dp)
                    } else {
                        Modifier
                    }
                )
                .background(MaterialTheme.colorScheme.surface),
    )
}
