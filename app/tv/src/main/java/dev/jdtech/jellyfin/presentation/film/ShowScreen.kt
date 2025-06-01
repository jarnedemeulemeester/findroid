package dev.jdtech.jellyfin.presentation.film

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyShow
import dev.jdtech.jellyfin.core.presentation.theme.Yellow
import dev.jdtech.jellyfin.film.presentation.show.ShowAction
import dev.jdtech.jellyfin.film.presentation.show.ShowState
import dev.jdtech.jellyfin.film.presentation.show.ShowViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.getShowDateString
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ShowScreen(
    showId: UUID,
    navigateToItem: (item: FindroidItem) -> Unit,
    navigateToPlayer: (items: ArrayList<PlayerItem>) -> Unit,
    viewModel: ShowViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadShow(showId)
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> navigateToPlayer(ArrayList(event.items))
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    ShowScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ShowAction.Play -> {
                    state.show?.let { show ->
                        playerViewModel.loadPlayerItems(show, startFromBeginning = action.startFromBeginning)
                    }
                }
                is ShowAction.PlayTrailer -> {
                    try {
                        uriHandler.openUri(action.trailer)
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                is ShowAction.NavigateToItem -> navigateToItem(action.item)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun ShowScreenLayout(
    state: ShowState,
    onAction: (ShowAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val locale = configuration.locales.get(0)

    val listState = rememberLazyListState()
    val listSize = remember { mutableIntStateOf(2) }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex)
    }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        state.show?.let { show ->
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
                    model = show.images.backdrop,
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
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 112.dp,
                        bottom = MaterialTheme.spacings.large,
                    ),
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
                                text = show.name,
                                style = MaterialTheme.typography.displayMedium,
                            )
                            if (show.originalTitle != show.name) {
                                show.originalTitle?.let { originalTitle ->
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
                                    text = getShowDateString(show),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    text = stringResource(CoreR.string.runtime_minutes, show.runtimeTicks.div(600000000)),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                show.officialRating?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                show.communityRating?.let {
                                    Row {
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_star),
                                            contentDescription = null,
                                            tint = Yellow,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.extraSmall))
                                        Text(
                                            text = String.format(locale, "%.1f", it),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                            Text(
                                text = show.overview,
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
                                        onAction(ShowAction.Play())
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
                                show.trailer?.let { trailerUri ->
                                    Button(
                                        onClick = {
                                            onAction(ShowAction.PlayTrailer(trailerUri))
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
                                        when (show.played) {
                                            true -> onAction(ShowAction.UnmarkAsPlayed)
                                            false -> onAction(ShowAction.MarkAsPlayed)
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(id = CoreR.drawable.ic_check),
                                        contentDescription = null,
                                        tint = if (show.played) Color.Red else LocalContentColor.current,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(id = if (show.played) CoreR.string.unmark_as_played else CoreR.string.mark_as_played))
                                }
                                Button(
                                    onClick = {
                                        when (show.favorite) {
                                            true -> onAction(ShowAction.UnmarkAsFavorite)
                                            false -> onAction(ShowAction.MarkAsFavorite)
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (show.favorite) CoreR.drawable.ic_heart_filled else CoreR.drawable.ic_heart),
                                        contentDescription = null,
                                        tint = if (show.favorite) Color.Red else LocalContentColor.current,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(id = if (show.favorite) CoreR.string.remove_from_favorites else CoreR.string.add_to_favorites))
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
                                        text = show.genres.joinToString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                state.director?.let { director ->
                                    Column {
                                        Text(
                                            text = stringResource(id = CoreR.string.director),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = .5f),
                                        )
                                        Text(
                                            text = director.name,
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
                                        text = state.writers.joinToString { it.name },
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
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2),
                        ) {
                            items(state.seasons) { season ->
                                ItemCard(
                                    item = season,
                                    direction = Direction.VERTICAL,
                                    onClick = {
                                        onAction(ShowAction.NavigateToItem(season))
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
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ShowScreenLayoutPreview() {
    FindroidTheme {
        ShowScreenLayout(
            state = ShowState(
                show = dummyShow,
                nextUp = dummyEpisode,
            ),
            onAction = {},
        )
    }
}
