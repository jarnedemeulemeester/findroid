package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.destinations.MovieScreenDestination
import dev.jdtech.jellyfin.ui.dummy.dummyHomeItems
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(key1 = true) {
        homeViewModel.loadData()
    }

    val delegatedUiState by homeViewModel.uiState.collectAsState()

    HomeScreenLayout(
        uiState = delegatedUiState,
        onClick = { item ->
            when (item) {
                is FindroidMovie -> {
                    navigator.navigate(MovieScreenDestination(item.id))
                }
            }
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeScreenLayout(
    uiState: HomeViewModel.UiState,
    onClick: (FindroidItem) -> Unit,
) {
    when (uiState) {
        is HomeViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
        is HomeViewModel.UiState.Normal -> {
            TvLazyColumn(
                contentPadding = PaddingValues(bottom = MaterialTheme.spacings.large),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.homeItems, key = { it.id }) { homeItem ->
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
                                items(homeItem.homeSection.items) { item ->
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
                                items(homeItem.view.items.orEmpty()) { item ->
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
                onClick = {},
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
