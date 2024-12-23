package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSection
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeView
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.home.HomeState
import dev.jdtech.jellyfin.film.presentation.home.HomeViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun HomeScreen(
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    navigateToPlayer: (items: ArrayList<PlayerItem>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isLoading: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadData()
    }

    LaunchedEffect(state.isLoading) {
        isLoading(state.isLoading)
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> navigateToPlayer(ArrayList(event.items))
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    HomeScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is HomeAction.OnItemClick -> {
                    when (action.item) {
                        is FindroidMovie -> navigateToMovie(action.item.id)
                        is FindroidShow -> navigateToShow(action.item.id)
                        is FindroidEpisode -> {
                            playerViewModel.loadPlayerItems(item = action.item)
                        }
                    }
                }
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun HomeScreenLayout(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.sections) {
        focusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacings.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
    ) {
        items(state.sections, key = { it.id }) { section ->
            Column(
                modifier = Modifier.animateItem(),
            ) {
                Text(
                    text = section.homeSection.name.asString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = MaterialTheme.spacings.large),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.large),
                ) {
                    items(section.homeSection.items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            direction = Direction.HORIZONTAL,
                            onClick = {
                                onAction(HomeAction.OnItemClick(it))
                            },
                        )
                    }
                }
            }
        }
        items(state.views, key = { it.id }) { view ->
            Column(
                modifier = Modifier.animateItem(),
            ) {
                Text(
                    text = stringResource(id = CoreR.string.latest_library, view.view.name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = MaterialTheme.spacings.large),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.large),
                ) {
                    items(view.view.items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            direction = Direction.VERTICAL,
                            onClick = {
                                onAction(HomeAction.OnItemClick(it))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        HomeScreenLayout(
            state = HomeState(
                sections = listOf(dummyHomeSection),
                views = listOf(dummyHomeView),
            ),
            onAction = {},
        )
    }
}
