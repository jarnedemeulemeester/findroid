package dev.jdtech.jellyfin.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.core.presentation.theme.Yellow
import dev.jdtech.jellyfin.film.presentation.movie.MovieAction
import dev.jdtech.jellyfin.film.presentation.movie.MovieState
import dev.jdtech.jellyfin.film.presentation.movie.MovieViewModel
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.format
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun MovieScreen(
    movieId: UUID,
    navigateToPlayer: (itemId: UUID) -> Unit,
    viewModel: MovieViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadMovie(movieId = movieId)
    }

    MovieScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is MovieAction.Play -> {
                    navigateToPlayer(movieId)
                }
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun MovieScreenLayout(
    state: MovieState,
    onAction: (MovieAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val locale = configuration.locales.get(0)

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        state.movie?.let { movie ->
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
                    model = movie.images.backdrop,
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
                        text = movie.name,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    if (movie.originalTitle != movie.name) {
                        movie.originalTitle?.let { originalTitle ->
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
                        movie.premiereDate?.let { premiereDate ->
                            Text(
                                text = premiereDate.format(),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Text(
                            text = stringResource(CoreR.string.runtime_minutes, movie.runtimeTicks.div(600000000)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        movie.officialRating?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        movie.communityRating?.let {
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
                        text = movie.overview,
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
                                onAction(MovieAction.Play(startFromBeginning = false))
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
                        movie.trailer?.let { trailerUri ->
                            Button(
                                onClick = {
                                    onAction(MovieAction.PlayTrailer(trailerUri))
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
                                when (movie.played) {
                                    true -> onAction(MovieAction.UnmarkAsPlayed)
                                    false -> onAction(MovieAction.MarkAsPlayed)
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_check),
                                contentDescription = null,
                                tint = if (movie.played) Color.Red else LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(id = if (movie.played) CoreR.string.unmark_as_played else CoreR.string.mark_as_played))
                        }
                        Button(
                            onClick = {
                                when (movie.favorite) {
                                    true -> onAction(MovieAction.UnmarkAsFavorite)
                                    false -> onAction(MovieAction.MarkAsFavorite)
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = if (movie.favorite) CoreR.drawable.ic_heart_filled else CoreR.drawable.ic_heart),
                                contentDescription = null,
                                tint = if (movie.favorite) Color.Red else LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(id = if (movie.favorite) CoreR.string.remove_from_favorites else CoreR.string.add_to_favorites))
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
                                text = movie.genres.joinToString(),
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
private fun MovieScreenLayoutPreview() {
    FindroidTheme {
        MovieScreenLayout(
            state = MovieState(
                movie = dummyMovie,
                videoMetadata = dummyVideoMetadata,
            ),
            onAction = {},
        )
    }
}
