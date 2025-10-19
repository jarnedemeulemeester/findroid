package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlinx.coroutines.delay

private val dynamicPageSize = object : PageSize {
    override fun Density.calculateMainAxisPageSize(
        availableSpace: Int,
        pageSpacing: Int,
    ): Int {
        val nPages = when {
            availableSpace.toDp() >= 840.dp -> 3
            availableSpace.toDp() >= 600.dp -> 2
            else -> 1
        }

        return (availableSpace - (nPages - 1) * pageSpacing) / nPages
    }
}

@Composable
fun HomeCarousel(
    items: List<JellyCastItem>,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val pagerIsDragged by pagerState.interactionSource.collectIsDraggedAsState()

    val autoScrollDelay = 5000L

    if (!pagerIsDragged) {
        LaunchedEffect(pagerState) {
            while (true) {
                delay(autoScrollDelay)
                val nextPage = if (pagerState.canScrollForward) {
                    pagerState.currentPage + 1
                } else {
                    0
                }
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = itemsPadding,
        pageSize = dynamicPageSize,
        pageSpacing = MaterialTheme.spacings.medium,
    ) { page ->
        val item = items[page]
        HomeCarouselItem(item = item, onAction = onAction)
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeCarouselPreview() {
    JellyCastTheme {
        HomeCarousel(
            items = dummyMovies,
            itemsPadding = PaddingValues(horizontal = 0.dp),
            onAction = {},
        )
    }
}
