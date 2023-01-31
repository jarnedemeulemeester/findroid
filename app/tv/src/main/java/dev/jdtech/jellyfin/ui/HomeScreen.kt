package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

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
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.homeItems, key = { it.id }) { homeItem ->
                    when (homeItem) {
                        is HomeItem.Libraries -> {
                            Text(
                                text = homeItem.section.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                items(homeItem.section.items) { library ->
                                    Column(
                                        modifier = Modifier
                                            .width(240.dp)
                                            .clickable { }
                                    ) {
                                        ItemPoster(
                                            item = library,
                                            api = api,
                                            direction = Direction.HORIZONTAL
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = library.name.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        is HomeItem.Section -> {
                            Text(
                                text = homeItem.homeSection.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                items(homeItem.homeSection.items) { item ->
                                    Column(
                                        modifier = Modifier
                                            .width(240.dp)
                                            .clickable { }
                                    ) {
                                        ItemPoster(
                                            item = item,
                                            api = api,
                                            direction = Direction.HORIZONTAL
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.name.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        is HomeItem.ViewItem -> {
                            Text(
                                text = homeItem.view.name.orEmpty(),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                items(homeItem.view.items.orEmpty()) { item ->
                                    Column(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable { }
                                    ) {
                                        ItemPoster(
                                            item = item,
                                            api = api,
                                            direction = Direction.VERTICAL
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.name.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

        }
        is HomeViewModel.UiState.Error -> {
            Text(text = uiState.error.toString())
        }
    }
}

fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

fun <K, V> Map<out K, V>?.isNotNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

enum class Direction {
    HORIZONTAL, VERTICAL
}

@Composable
fun ItemPoster(item: BaseItemDto, api: JellyfinApi, direction: Direction) {
    var itemId = item.id
    var imageType = ImageType.PRIMARY

    if (direction == Direction.HORIZONTAL) {
        if (item.imageTags.isNotNullOrEmpty()) { // TODO: Downloadmetadata currently does not store imagetags, so it always uses the backdrop
            when (item.type) {
                BaseItemKind.MOVIE -> {
                    if (item.backdropImageTags.isNotNullOrEmpty()) {
                        imageType = ImageType.BACKDROP
                    }
                }
                else -> {
                    if (!item.imageTags!!.keys.contains(ImageType.PRIMARY)) {
                        imageType = ImageType.BACKDROP
                    }
                }
            }
        } else {
            if (item.type == BaseItemKind.EPISODE) {
                itemId = item.seriesId!!
                imageType = ImageType.BACKDROP
            }
        }
    } else {
        itemId =
            if (item.type == BaseItemKind.EPISODE || item.type == BaseItemKind.SEASON && item.imageTags.isNullOrEmpty()) item.seriesId
                ?: item.id else item.id
    }


    AsyncImage(
        model = "${api.api.baseUrl}/items/$itemId/Images/$imageType",
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (direction == Direction.HORIZONTAL) 1.77f else 0.66f)
            .clip(MaterialTheme.shapes.large)
            .background(
                MaterialTheme.colorScheme.surface
            )
    )
}