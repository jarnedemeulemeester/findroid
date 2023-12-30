package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.models.EpisodeItem
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.ui.components.EpisodeCard
import dev.jdtech.jellyfin.ui.dummy.dummyEpisodeItems
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.viewmodels.SeasonViewModel
import java.util.UUID

@Destination
@Composable
fun SeasonScreen(
    navigator: DestinationsNavigator,
    seriesId: UUID,
    seasonId: UUID,
    seriesName: String,
    seasonName: String,
    seasonViewModel: SeasonViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        seasonViewModel.loadEpisodes(
            seriesId = seriesId,
            seasonId = seasonId,
            offline = false,
        )
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                navigator.navigate(PlayerActivityDestination(items = ArrayList(event.items)))
            }
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    val delegatedUiState by seasonViewModel.uiState.collectAsState()

    SeasonScreenLayout(
        seriesName = seriesName,
        seasonName = seasonName,
        uiState = delegatedUiState,
        onClick = { episode ->
            playerViewModel.loadPlayerItems(item = episode)
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeasonScreenLayout(
    seriesName: String,
    seasonName: String,
    uiState: SeasonViewModel.UiState,
    onClick: (FindroidEpisode) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is SeasonViewModel.UiState.Loading -> Text(text = "LOADING")
        is SeasonViewModel.UiState.Normal -> {
            val episodes = uiState.episodes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF001721)))),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                start = MaterialTheme.spacings.extraLarge,
                                top = MaterialTheme.spacings.large,
                                end = MaterialTheme.spacings.large,
                            ),
                    ) {
                        Text(
                            text = seasonName,
                            style = MaterialTheme.typography.displayMedium,
                        )
                        Text(
                            text = seriesName,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    TvLazyColumn(
                        contentPadding = PaddingValues(
                            top = MaterialTheme.spacings.large,
                            bottom = MaterialTheme.spacings.large,
                        ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                        modifier = Modifier
                            .weight(2f)
                            .padding(end = MaterialTheme.spacings.extraLarge)
                            .focusRequester(focusRequester),
                    ) {
                        items(episodes) { episodeItem ->
                            when (episodeItem) {
                                is EpisodeItem.Episode -> {
                                    EpisodeCard(episode = episodeItem.episode, onClick = { onClick(episodeItem.episode) })
                                }

                                else -> Unit
                            }
                        }
                    }

                    LaunchedEffect(true) {
                        focusRequester.requestFocus()
                    }
                }
            }
        }
        is SeasonViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SeasonScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            SeasonScreenLayout(
                seriesName = "86 EIGHTY-SIX",
                seasonName = "Season 1",
                uiState = SeasonViewModel.UiState.Normal(dummyEpisodeItems),
                onClick = {},
            )
        }
    }
}
