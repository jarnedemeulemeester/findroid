package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.ui.destinations.LibraryScreenDestination
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import org.jellyfin.sdk.model.api.ImageType

@OptIn(ExperimentalTvMaterial3Api::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        homeViewModel.loadData(includeLibraries = true)
    }

    val api = JellyfinApi.getInstance(context)

    val delegatedUiState by homeViewModel.uiState.collectAsState()
    when (val uiState = delegatedUiState) {
        is HomeViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
        is HomeViewModel.UiState.Normal -> {
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Header(
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                items(uiState.homeItems, key = { it.id }) { homeItem ->
                    when (homeItem) {
                        is HomeItem.Libraries -> {
                            Text(
                                text = homeItem.section.name.asString(context.resources),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp)
                            ) {
                                items(homeItem.section.items) { library ->
                                    CompactCard(
                                        onClick = {
                                            navigator.navigate(
                                                LibraryScreenDestination(
                                                    library.id,
                                                    (library as FindroidCollection).type
                                                )
                                            )
                                        },
                                        image = {
                                            ItemPoster(
                                                item = library,
                                                api = api,
                                                direction = Direction.HORIZONTAL
                                            )
                                        },
                                        title = {
                                            Text(
                                                text = library.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(8.dp),
                                            )
                                        },
                                        modifier = Modifier.width(240.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        is HomeItem.Section -> {
                            Text(
                                text = homeItem.homeSection.name.asString(context.resources),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp)
                            ) {
                                items(homeItem.homeSection.items) { item ->
                                    Card(
                                        colors = CardDefaults.colors(containerColor = Color.Transparent),
                                        onClick = { /*TODO*/ }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(180.dp)
                                        ) {
                                            Box {
                                                ItemPoster(
                                                    item = item,
                                                    api = api,
                                                    direction = Direction.HORIZONTAL
                                                )
                                                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                                    Row {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .height(4.dp)
                                                                .width(
                                                                    item.playbackPositionTicks
                                                                        .div(item.runtimeTicks.toFloat())
                                                                        .times(
                                                                            1.64
                                                                        ).dp
                                                                )
                                                                .clip(MaterialTheme.shapes.extraSmall)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (item is FindroidEpisode) item.seriesName else item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            if (item is FindroidEpisode) {
                                                Text(
                                                    text = stringResource(id = CoreR.string.episode_name_extended, item.parentIndexNumber, item.indexNumber, item.name),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        is HomeItem.ViewItem -> {
                            Text(
                                text = homeItem.view.name.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp)
                            ) {
                                items(homeItem.view.items.orEmpty()) { item ->
                                    Card (
                                        colors = CardDefaults.colors(containerColor = Color.Transparent),
                                        onClick = { /*TODO*/ }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(120.dp)
                                        ) {
                                            ItemPoster(
                                                item = item,
                                                api = api,
                                                direction = Direction.VERTICAL
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        else -> Unit
                    }
                }
            }
        }
        is HomeViewModel.UiState.Error -> {
            Text(text = uiState.error.toString())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Header(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Image(
            painter = painterResource(id = CoreR.drawable.ic_banner),
            contentDescription = null,
            modifier = Modifier.height(40.dp)
        )
    }
}

enum class Direction {
    HORIZONTAL, VERTICAL
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ItemPoster(item: FindroidItem, api: JellyfinApi, direction: Direction) {
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
        model = "${api.api.baseUrl}/items/$itemId/Images/$imageType",
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (direction == Direction.HORIZONTAL) 1.77f else 0.66f)
            .background(
                MaterialTheme.colorScheme.surface
            )
    )
}
