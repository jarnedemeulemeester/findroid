package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.components.ItemPoster
import dev.jdtech.jellyfin.ui.dummy.dummyHomeItems
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        homeViewModel.loadData()
    }

    val api = JellyfinApi.getInstance(context)

    val delegatedUiState by homeViewModel.uiState.collectAsState()

    HomeScreenLayout(
        uiState = delegatedUiState,
        baseUrl = api.api.baseUrl ?: "",
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeScreenLayout(
    uiState: HomeViewModel.UiState,
    baseUrl: String,
) {
    when (uiState) {
        is HomeViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
        is HomeViewModel.UiState.Normal -> {
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.homeItems, key = { it.id }) { homeItem ->
                    when (homeItem) {
                        is HomeItem.Section -> {
                            Text(
                                text = homeItem.homeSection.name.asString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 32.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                            ) {
                                items(homeItem.homeSection.items) { item ->
                                    ItemCard(item = item, baseUrl = baseUrl, onClick = {})
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        is HomeItem.ViewItem -> {
                            Text(
                                text = homeItem.view.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 32.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                            ) {
                                items(homeItem.view.items.orEmpty()) { item ->
                                    Card(
                                        colors = CardDefaults.colors(
                                            containerColor = Color.Transparent,
                                        ),
                                        onClick = { /*TODO*/ },
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(120.dp),
                                        ) {
                                            ItemPoster(
                                                item = item,
                                                baseUrl = baseUrl,
                                                direction = Direction.VERTICAL,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            HomeScreenLayout(
                uiState = HomeViewModel.UiState.Normal(dummyHomeItems),
                baseUrl = "https://demo.jellyfin.org/stable",
            )
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
            .height(80.dp),
    ) {
        Image(
            painter = painterResource(id = CoreR.drawable.ic_banner),
            contentDescription = null,
            modifier = Modifier.height(40.dp),
        )
    }
}
