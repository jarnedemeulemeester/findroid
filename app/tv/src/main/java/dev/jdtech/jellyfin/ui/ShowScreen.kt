package dev.jdtech.jellyfin.ui

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.destinations.SeasonScreenDestination
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.dummy.dummyShow
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.Yellow
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.viewmodels.ShowViewModel
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun ShowScreen(
    navigator: DestinationsNavigator,
    itemId: UUID,
    showViewModel: ShowViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        showViewModel.loadData(itemId, false)
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                navigator.safeNavigate(PlayerActivityDestination(items = ArrayList(event.items)))
            }
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    val delegatedUiState by showViewModel.uiState.collectAsState()

    ShowScreenLayout(
        uiState = delegatedUiState,
        onPlayClick = {
            playerViewModel.loadPlayerItems(showViewModel.item)
        },
        onTrailerClick = { trailerUri ->
            try {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(trailerUri),
                ).also {
                    context.startActivity(it)
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        },
        onPlayedClick = {
            showViewModel.togglePlayed()
        },
        onFavoriteClick = {
            showViewModel.toggleFavorite()
        },
        onSeasonClick = { season ->
            navigator.safeNavigate(SeasonScreenDestination(seriesId = season.seriesId, seasonId = season.id, seriesName = season.seriesName, seasonName = season.name))
        },
    )
}

@Composable
private fun ShowScreenLayout(
    uiState: ShowViewModel.UiState,
    onPlayClick: () -> Unit,
    onTrailerClick: (String) -> Unit,
    onPlayedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSeasonClick: (FindroidSeason) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    val listState = rememberTvLazyListState()
    val listSize = remember { mutableIntStateOf(2) }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex)
    }

    when (uiState) {
        is ShowViewModel.UiState.Loading -> Text(text = "LOADING")
        is ShowViewModel.UiState.Normal -> {
            val item = uiState.item
            val seasons = uiState.seasons
            var size by remember {
                mutableStateOf(Size.Zero)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        size = coordinates.size.toSize()
                    },
            ) {
                AsyncImage(
                    model = item.images.backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize(),
                )
                if (size != Size.Zero) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color.Black.copy(alpha = .2f), Color.Black),
                                    center = Offset(size.width, 0f),
                                    radius = size.width * .8f,
                                ),
                            ),
                    )
                }
                TvLazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 112.dp, bottom = MaterialTheme.spacings.large),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    userScrollEnabled = false,
                    modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                        when (keyEvent.key.nativeKeyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                currentIndex = (++currentIndex).coerceIn(0, listSize.intValue - 1)
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                currentIndex = (--currentIndex).coerceIn(0, listSize.intValue - 1)
                            }
                        }
                        false
                    },
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .padding(
                                    start = MaterialTheme.spacings.default * 2,
                                    end = MaterialTheme.spacings.default * 2,
                                ),
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.displayMedium,
                            )
                            if (item.originalTitle != item.name) {
                                item.originalTitle?.let { originalTitle ->
                                    Text(
                                        text = originalTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                            ) {
                                Text(
                                    text = uiState.dateString,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    text = uiState.runTime,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                item.officialRating?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                item.communityRating?.let {
                                    Row {
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_star),
                                            contentDescription = null,
                                            tint = Yellow,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.extraSmall))
                                        Text(
                                            text = String.format("%.1f", item.communityRating),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                            Text(
                                text = item.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(640.dp),
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                            ) {
                                Button(
                                    onClick = {
                                        onPlayClick()
                                    },
                                    modifier = Modifier.focusRequester(focusRequester),
                                ) {
                                    Icon(
                                        painter = painterResource(id = CoreR.drawable.ic_play),
                                        contentDescription = null,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(id = CoreR.string.play))
                                }
                                item.trailer?.let { trailerUri ->
                                    Button(
                                        onClick = {
                                            onTrailerClick(trailerUri)
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_film),
                                            contentDescription = null,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(id = CoreR.string.watch_trailer))
                                    }
                                }
                                Button(
                                    onClick = {
                                        onPlayedClick()
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(id = CoreR.drawable.ic_check),
                                        contentDescription = null,
                                        tint = if (item.played) Color.Red else LocalContentColor.current,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(id = if (item.played) CoreR.string.unmark_as_played else CoreR.string.mark_as_played))
                                }
                                Button(
                                    onClick = {
                                        onFavoriteClick()
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (item.favorite) CoreR.drawable.ic_heart_filled else CoreR.drawable.ic_heart),
                                        contentDescription = null,
                                        tint = if (item.favorite) Color.Red else LocalContentColor.current,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(id = if (item.favorite) CoreR.string.remove_from_favorites else CoreR.string.add_to_favorites))
                                }
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(id = CoreR.string.genres),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = .5f),
                                    )
                                    Text(
                                        text = uiState.genresString,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                uiState.director?.let { director ->
                                    Column {
                                        Text(
                                            text = stringResource(id = CoreR.string.director),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = .5f),
                                        )
                                        Text(
                                            text = director.name ?: "Unknown",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(id = CoreR.string.writers),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = .5f),
                                    )
                                    Text(
                                        text = uiState.writersString,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
                            Text(
                                text = stringResource(id = CoreR.string.seasons),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                    item {
                        TvLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2),
                        ) {
                            items(seasons) { season ->
                                ItemCard(
                                    item = season,
                                    direction = Direction.VERTICAL,
                                    onClick = {
                                        onSeasonClick(season)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        }

        is ShowViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ShowScreenLayoutPreview() {
    FindroidTheme {
        ShowScreenLayout(
            uiState = ShowViewModel.UiState.Normal(
                item = dummyShow,
                actors = emptyList(),
                director = null,
                writers = emptyList(),
                writersString = "Hiroshi Seko, Hajime Isayama",
                genresString = "Action, Science Fiction, Adventure",
                runTime = "0 min",
                dateString = "2013 - 2023",
                nextUp = null,
                seasons = emptyList(),
            ),
            onPlayClick = {},
            onTrailerClick = {},
            onPlayedClick = {},
            onFavoriteClick = {},
            onSeasonClick = {},
        )
    }
}
