package dev.jdtech.jellyfin.presentation.film

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.film.presentation.movie.MovieAction
import dev.jdtech.jellyfin.film.presentation.movie.MovieState
import dev.jdtech.jellyfin.film.presentation.movie.MovieViewModel
import dev.jdtech.jellyfin.presentation.film.components.ActorsRow
import dev.jdtech.jellyfin.presentation.film.components.InfoText
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun MovieScreen(
    movieId: UUID,
    navigateBack: () -> Unit,
    navigateToPerson: (personId: UUID) -> Unit,
    viewModel: MovieViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    var isLoadingPlayer by remember { mutableStateOf(false) }
    var isLoadingRestartPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        viewModel.loadMovie(movieId = movieId)
    }

    MovieScreenLayout(
        state = state,
        isLoadingPlayer = isLoadingPlayer,
        isLoadingRestartPlayer = isLoadingRestartPlayer,
        onAction = { action ->
            when (action) {
                is MovieAction.Play -> {
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("itemId", movieId.toString())
                    context.startActivity(intent)
                }
                is MovieAction.PlayTrailer -> {
                    try {
                        uriHandler.openUri(action.trailer)
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                is MovieAction.OnBackClick -> navigateBack()
                is MovieAction.NavigateToPerson -> navigateToPerson(action.personId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun MovieScreenLayout(
    state: MovieState,
    isLoadingPlayer: Boolean,
    isLoadingRestartPlayer: Boolean,
    onAction: (MovieAction) -> Unit,
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        state.movie?.let { movie ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                ItemHeader(
                    item = movie,
                    scrollState = scrollState,
                    content = {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = paddingStart,
                                    end = paddingEnd,
                                ),
                        ) {
                            Text(
                                text = movie.name,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            movie.originalTitle?.let { originalTitle ->
                                if (originalTitle != movie.name) {
                                    Text(
                                        text = originalTitle,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    },
                )
                Column(
                    modifier = Modifier.padding(
                        start = paddingStart,
                        end = paddingEnd,
                    ),
                ) {
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        movie.premiereDate?.let { premiereDate ->
                            Text(
                                text = premiereDate.year.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = stringResource(CoreR.string.runtime_minutes, movie.runtimeTicks.div(600000000)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        movie.officialRating?.let { officialRating ->
                            Text(
                                text = officialRating,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        movie.communityRating?.let { communityRating ->
                            Row(
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_star),
                                    contentDescription = null,
                                    tint = Color("#F2C94C".toColorInt()),
                                )
                                Spacer(Modifier.width(MaterialTheme.spacings.extraSmall))
                                Text(
                                    text = "%.1f".format(communityRating),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                    state.videoMetadata?.let { videoMetadata ->
                        VideoMetadataBar(videoMetadata)
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                    }
                    ItemButtonsBar(
                        item = movie,
                        onPlayClick = { startFromBeginning ->
                            onAction(MovieAction.Play(startFromBeginning = startFromBeginning))
                        },
                        onMarkAsPlayedClick = {
                            when (movie.played) {
                                true -> onAction(MovieAction.UnmarkAsPlayed)
                                false -> onAction(MovieAction.MarkAsPlayed)
                            }
                        },
                        onMarkAsFavoriteClick = {
                            when (movie.favorite) {
                                true -> onAction(MovieAction.UnmarkAsFavorite)
                                false -> onAction(MovieAction.MarkAsFavorite)
                            }
                        },
                        onTrailerClick = { uri ->
                            onAction(MovieAction.PlayTrailer(uri))
                        },
                        onDownloadClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        isLoadingPlayer = isLoadingPlayer,
                        isLoadingRestartPlayer = isLoadingRestartPlayer,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                    OverviewText(
                        text = movie.overview,
                        maxCollapsedLines = 3,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium))
                    InfoText(
                        genres = movie.genres,
                        director = state.director,
                        writers = state.writers,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium))
                }
                if (state.actors.isNotEmpty()) {
                    ActorsRow(
                        actors = state.actors,
                        onActorClick = { personId ->
                            onAction(MovieAction.NavigateToPerson(personId))
                        },
                        contentPadding = PaddingValues(
                            start = paddingStart,
                            end = paddingEnd,
                        ),
                    )
                }
                Spacer(Modifier.height(paddingBottom))
            }
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = safePadding.start + MaterialTheme.spacings.small,
                    top = safePadding.top + MaterialTheme.spacings.small,
                    end = safePadding.end + MaterialTheme.spacings.small,
                ),
        ) {
            IconButton(
                onClick = { onAction(MovieAction.OnBackClick) },
                modifier = Modifier
                    .alpha(0.7f),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                    contentDescription = null,
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        MovieScreenLayout(
            state = MovieState(
                movie = dummyMovie,
                videoMetadata = dummyVideoMetadata,
            ),
            isLoadingPlayer = false,
            isLoadingRestartPlayer = false,
            onAction = {},
        )
    }
}
