package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.MovieScreenDestination
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.destinations.ShowScreenDestination
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.dummy.dummyHomeItems
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isLoading: (Boolean) -> Unit,
) {
    LaunchedEffect(key1 = true) {
        homeViewModel.loadData()
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                navigator.navigate(PlayerActivityDestination(items = ArrayList(event.items)))
            }
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    val delegatedUiState by homeViewModel.uiState.collectAsState()

    HomeScreenLayout(
        uiState = delegatedUiState,
        isLoading = isLoading,
        onClick = { item ->
            when (item) {
                is FindroidMovie -> {
                    navigator.navigate(MovieScreenDestination(item.id))
                }
                is FindroidShow -> {
                    navigator.navigate(ShowScreenDestination(item.id))
                }
                is FindroidEpisode -> {
                    playerViewModel.loadPlayerItems(item = item)
                }
            }
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeScreenLayout(
    uiState: HomeViewModel.UiState,
    isLoading: (Boolean) -> Unit,
    onClick: (FindroidItem) -> Unit,
) {
    var homeItems: List<HomeItem> by remember { mutableStateOf(emptyList()) }

    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is HomeViewModel.UiState.Normal -> {
            homeItems = uiState.homeItems
            isLoading(false)
        }
        is HomeViewModel.UiState.Loading -> {
            isLoading(true)
        }
        else -> Unit
    }
    TvLazyColumn(
        contentPadding = PaddingValues(bottom = MaterialTheme.spacings.large),
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
    ) {
        items(homeItems, key = { it.id }) { homeItem ->
            when (homeItem) {
                is HomeItem.Section -> {
                    Text(
                        text = homeItem.homeSection.name.asString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = MaterialTheme.spacings.large),
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.large),
                    ) {
                        items(homeItem.homeSection.items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                direction = Direction.HORIZONTAL,
                                onClick = {
                                    onClick(it)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
                }
                is HomeItem.ViewItem -> {
                    Text(
                        text = stringResource(id = CoreR.string.latest_library, homeItem.view.name),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = MaterialTheme.spacings.large),
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.large),
                    ) {
                        items(homeItem.view.items.orEmpty(), key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                direction = Direction.VERTICAL,
                                onClick = {
                                    onClick(it)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(homeItems) {
        focusRequester.requestFocus()
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        HomeScreenLayout(
            uiState = HomeViewModel.UiState.Normal(dummyHomeItems),
            isLoading = {},
            onClick = {},
        )
    }
}
