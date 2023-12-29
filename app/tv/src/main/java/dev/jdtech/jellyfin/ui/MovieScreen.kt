package dev.jdtech.jellyfin.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.PlayerActivityDestination
import dev.jdtech.jellyfin.models.AudioChannel
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.Resolution
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.ui.dummy.dummyMovie
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.Yellow
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.MovieViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import org.jellyfin.sdk.model.api.BaseItemPerson
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun MovieScreen(
    navigator: DestinationsNavigator,
    itemId: UUID,
    movieViewModel: MovieViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        movieViewModel.loadData(itemId)
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                navigator.navigate(PlayerActivityDestination(items = ArrayList(event.items)))
            }
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    val delegatedUiState by movieViewModel.uiState.collectAsState()

    MovieScreenLayout(
        uiState = delegatedUiState,
        onPlayClick = {
            playerViewModel.loadPlayerItems(movieViewModel.item)
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
            movieViewModel.togglePlayed()
        },
        onFavoriteClick = {
            movieViewModel.toggleFavorite()
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieScreenLayout(
    uiState: MovieViewModel.UiState,
    onPlayClick: () -> Unit,
    onTrailerClick: (String) -> Unit,
    onPlayedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is MovieViewModel.UiState.Loading -> Text(text = "LOADING")
        is MovieViewModel.UiState.Normal -> {
            val item = uiState.item
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
                Column(
                    modifier = Modifier
                        .padding(start = MaterialTheme.spacings.default * 2, end = MaterialTheme.spacings.default * 2),
                ) {
                    Spacer(modifier = Modifier.height(112.dp))
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
//                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
//                    Text(
//                        text = stringResource(id = CoreR.string.cast_amp_crew),
//                        style = MaterialTheme.typography.headlineMedium,
//                    )
                }
            }

            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        }

        is MovieViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun MovieScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            MovieScreenLayout(
                uiState = MovieViewModel.UiState.Normal(
                    item = dummyMovie,
                    actors = emptyList(),
                    director = BaseItemPerson(
                        id = UUID.randomUUID(),
                        name = "Robert Rodriguez",
                    ),
                    writers = emptyList(),
                    videoMetadata = VideoMetadata(
                        resolution = listOf(Resolution.UHD),
                        displayProfiles = listOf(DisplayProfile.HDR10),
                        audioChannels = listOf(AudioChannel.CH_5_1),
                        audioCodecs = listOf(AudioCodec.EAC3),
                        isAtmos = listOf(false),
                    ),
                    writersString = "James Cameron, Laeta Kalogridis, Yukito Kishiro",
                    genresString = "Action, Science Fiction, Adventure",
                    videoString = "",
                    audioString = "",
                    subtitleString = "",
                    runTime = "121 min",
                    dateString = "2019",
                ),
                onPlayClick = {},
                onTrailerClick = {},
                onPlayedClick = {},
                onFavoriteClick = {},
            )
        }
    }
}
