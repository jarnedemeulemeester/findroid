package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Carousel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeCarousel(
    items: List<FindroidItem>,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Carousel(
        itemCount = items.size,
        modifier = modifier.height(300.dp).fillMaxWidth(),
        contentTransformEndToStart = fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000))),
        contentTransformStartToEnd = fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000))),
    ) { itemIndex ->
        val item = items[itemIndex]
        HomeCarouselItem(item = item, onAction = onAction)
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeCarouselPreview() {
    FindroidTheme { HomeCarousel(items = dummyMovies, onAction = {}) }
}
