package dev.jdtech.jellyfin.presentation.film

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.TrailerActivity
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
import dev.jdtech.jellyfin.presentation.components.DlnaDevicePicker
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.dialogs.getStorageSelectionDialog
import dev.jdtech.jellyfin.presentation.downloads.DownloaderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dev.jdtech.jellyfin.cast.CastHelper
import dev.jdtech.jellyfin.dlna.DlnaHelper
import dev.jdtech.jellyfin.roku.RokuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

fun isExternalPlayerEnabled(context: Context): Boolean {
    val prefsName = context.packageName + "_preferences"
    val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("pref_player_external", false)
}

fun getSelectedExternalPlayer(context: Context): String? {
    val prefsName = context.packageName + "_preferences"
    val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    return sharedPreferences.getString("pref_player_external_app", null)
}

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

    LaunchedEffect(true) {
        viewModel.loadMovie(movieId = movieId)
    }

    MovieScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is MovieAction.Play -> {
                    // Check devices in order: Roku -> DLNA -> Chromecast -> Local
                    if (RokuHelper.isRokuDeviceAvailable()) {
                        // Send to Roku device
                        state.movie?.let { movie ->
                            try {
                                val streamUrl = movie.sources.firstOrNull()?.path ?: return@let
                                val positionMs = if (action.startFromBeginning) {
                                    0L
                                } else {
                                    movie.playbackPositionTicks / 10000
                                }
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    val success = RokuHelper.playMedia(
                                        contentUrl = streamUrl,
                                        title = movie.name,
                                        subtitle = movie.overview.orEmpty(),
                                        imageUrl = movie.images.primary.toString(),
                                        position = positionMs
                                    )
                                    
                                    launch(Dispatchers.Main) {
                                        if (success) {
                                            Toast.makeText(context, "Enviando a Roku...", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Error al enviar a Roku", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error sending to Roku")
                                Toast.makeText(context, "Error al enviar a Roku", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (DlnaHelper.isDlnaDeviceAvailable(context)) {
                        // Send to DLNA device
                        state.movie?.let { movie ->
                            try {
                                val streamUrl = movie.sources.firstOrNull()?.path ?: return@let
                                val applicationContext = context.applicationContext
                                // Convert ticks to milliseconds (1 tick = 100 nanoseconds = 0.0001 ms)
                                val positionMs = if (action.startFromBeginning) {
                                    0L
                                } else {
                                    movie.playbackPositionTicks / 10000
                                }
                                val durationMs = movie.runtimeTicks / 10000
                                DlnaHelper.loadMedia(
                                    context = applicationContext,
                                    contentUrl = streamUrl,
                                    contentType = "video/*",
                                    title = movie.name,
                                    subtitle = movie.overview.orEmpty(),
                                    imageUrl = movie.images.primary.toString(),
                                    position = positionMs,
                                    duration = durationMs
                                )
                                Toast.makeText(context, "Enviando a DLNA...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Timber.e(e, "Error sending to DLNA")
                                Toast.makeText(context, "Error al enviar a DLNA", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (CastHelper.isCastSessionAvailable(context)) {
                        // Send to Chromecast
                        state.movie?.let { movie ->
                            try {
                                val streamUrl = movie.sources.firstOrNull()?.path ?: return@let
                                val applicationContext = context.applicationContext
                                // Convert ticks to milliseconds (1 tick = 100 nanoseconds = 0.0001 ms)
                                val positionMs = if (action.startFromBeginning) {
                                    0L
                                } else {
                                    movie.playbackPositionTicks / 10000
                                }
                                CastHelper.loadMedia(
                                    context = applicationContext,
                                    contentUrl = streamUrl,
                                    contentType = "video/*",
                                    title = movie.name,
                                    subtitle = movie.overview.orEmpty(),
                                    imageUrl = movie.images.primary.toString(),
                                    position = positionMs
                                )
                                Toast.makeText(context, "Enviando a Chromecast...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Timber.e(e, "Error sending to Chromecast")
                                Toast.makeText(context, "Error al enviar a Chromecast", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Play locally or with external player
                        if (isExternalPlayerEnabled(context)) {
                            // Use external player
                            state.movie?.let { movie ->
                                try {
                                    val streamUrl = movie.sources.firstOrNull()?.path
                                    if (streamUrl != null) {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.setDataAndType(Uri.parse(streamUrl), "video/*")
                                        intent.putExtra("title", movie.name)
                                        intent.putExtra("position", if (action.startFromBeginning) 0L else movie.playbackPositionTicks / 10000)
                                        
                                        val selectedPlayer = getSelectedExternalPlayer(context)
                                        if (selectedPlayer != null) {
                                            // Use the selected player directly
                                            intent.setPackage(selectedPlayer)
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Timber.e(e, "Selected player not available, showing chooser")
                                                // If the selected player is not available, show chooser
                                                intent.setPackage(null)
                                                if (intent.resolveActivity(context.packageManager) != null) {
                                                    context.startActivity(Intent.createChooser(intent, context.getString(dev.jdtech.jellyfin.settings.R.string.select_external_player)))
                                                } else {
                                                    Toast.makeText(context, "No se encontró ningún reproductor externo", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            // No player selected, show chooser
                                            if (intent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(Intent.createChooser(intent, context.getString(dev.jdtech.jellyfin.settings.R.string.select_external_player)))
                                            } else {
                                                Toast.makeText(context, "No se encontró ningún reproductor externo", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Error: No se encontró URL de reproducción", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Error launching external player")
                                    Toast.makeText(context, "Error al abrir reproductor externo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Use internal player
                            val intent = Intent(context, PlayerActivity::class.java)
                            intent.putExtra("itemId", movieId.toString())
                            intent.putExtra("itemKind", BaseItemKind.MOVIE.serialName)
                            intent.putExtra("startFromBeginning", action.startFromBeginning)
                            context.startActivity(intent)
                        }
                    }
                }
                is MovieAction.PlayTrailer -> {
                    try {
                        // Play trailer in a WebView (for YouTube trailers)
                        val intent = Intent(context, TrailerActivity::class.java)
                        intent.putExtra("trailerUrl", action.trailer)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Error playing trailer")
                        Toast.makeText(context, "Error al reproducir el trailer", Toast.LENGTH_SHORT).show()
                    }
                }
                is MovieAction.Download -> {
                    state.movie?.let { movie ->
                        val dialog = getStorageSelectionDialog(
                            context = context,
                            onItemSelected = { which ->
                                // Use Hilt entry point to get Downloader from Application graph
                                val appContext = context.applicationContext
                                val downloader = EntryPointAccessors.fromApplication(appContext, DownloaderEntryPoint::class.java).downloader()
                                CoroutineScope(Dispatchers.IO).launch {
                                    val sourceId = movie.sources.firstOrNull()?.id ?: return@launch
                                    val result = downloader.downloadItem(movie, sourceId, which)
                                    if (result.first > 0) {
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(CoreR.string.download_started), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(context, result.second?.asString(context.resources) ?: context.getString(CoreR.string.unknown_error), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            onCancel = { /* no-op */ },
                        )
                        dialog.show()
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
    onAction: (MovieAction) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val safePadding = rememberSafePadding()
    
    var showDlnaDevicePicker by remember { mutableStateOf(false) }

    val paddingTop = safePadding.top
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
                    paddingTop = paddingTop,
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
                        onDlnaClick = {
                            if (DlnaHelper.isDlnaDeviceAvailable(appContext) || RokuHelper.isRokuDeviceAvailable()) {
                                // If already connected, disconnect both
                                DlnaHelper.stopDlna(appContext)
                                RokuHelper.disconnect()
                            } else {
                                // Show device picker (includes both DLNA and Roku devices)
                                showDlnaDevicePicker = true
                            }
                        },
                        onTrailerClick = { uri -> onAction(MovieAction.PlayTrailer(uri)) },
                        onDownloadClick = { onAction(MovieAction.Download) },
                        onDeleteClick = {
                            val local = movie.sources.firstOrNull { it.type == dev.jdtech.jellyfin.models.FindroidSourceType.LOCAL }
                            if (local != null) {
                                val downloader = EntryPointAccessors.fromApplication(appContext, DownloaderEntryPoint::class.java).downloader()
                                CoroutineScope(Dispatchers.IO).launch {
                                    downloader.deleteItem(movie, local)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
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
            horizontalArrangement = Arrangement.SpaceBetween,
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
            
            dev.jdtech.jellyfin.presentation.components.CastButton()
        }
        
        if (showDlnaDevicePicker) {
            DlnaDevicePicker(
                onDeviceSelected = {
                    showDlnaDevicePicker = false
                },
                onDismiss = {
                    showDlnaDevicePicker = false
                }
            )
        }
    }
}

@PreviewScreenSizes
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
