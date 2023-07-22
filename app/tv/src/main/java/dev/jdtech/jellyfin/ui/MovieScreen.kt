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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import dev.jdtech.jellyfin.models.AudioChannel
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.Resolution
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.ui.dummy.dummyMovie
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.Yellow
import dev.jdtech.jellyfin.viewmodels.MovieViewModel
import org.jellyfin.sdk.model.api.BaseItemPerson
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun MovieScreen(
    itemId: UUID,
    movieViewModel: MovieViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        movieViewModel.loadData(itemId)
    }

    val delegatedUiState by movieViewModel.uiState.collectAsState()

    MovieScreenLayout(
        uiState = delegatedUiState,
        onPlayClick = {},
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
        onPlayedClick = {},
        onFavoriteClick = {},
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
                        .padding(start = 48.dp, top = 112.dp, end = 48.dp),
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", item.communityRating),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(640.dp),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Button(
                            onClick = { },
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
                            onClick = { },
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_check),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(id = CoreR.string.mark_as_played))
                        }
                        Button(
                            onClick = { },
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_heart),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(id = CoreR.string.add_to_favorites))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
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
//                    Spacer(modifier = Modifier.height(32.dp))
//                    Text(
//                        text = stringResource(id = CoreR.string.cast_amp_crew),
//                        style = MaterialTheme.typography.headlineMedium,
//                    )
                }
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
