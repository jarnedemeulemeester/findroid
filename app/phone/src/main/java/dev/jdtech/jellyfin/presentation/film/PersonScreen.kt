package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.core.presentation.dummy.dummyPersonDetail
import dev.jdtech.jellyfin.film.presentation.person.PersonAction
import dev.jdtech.jellyfin.film.presentation.person.PersonState
import dev.jdtech.jellyfin.film.presentation.person.PersonViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import java.util.UUID

@Composable
fun PersonScreen(
    personId: UUID,
    navigateBack: () -> Unit,
    navigateToItem: (item: FindroidItem) -> Unit,
    viewModel: PersonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadPerson(personId) }

    PersonScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is PersonAction.NavigateBack -> navigateBack()
                is PersonAction.NavigateToItem -> navigateToItem(action.item)
            }
        },
    )
}

@Composable
private fun PersonScreenLayout(state: PersonState, onAction: (PersonAction) -> Unit) {
    val safePadding = rememberSafePadding()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingTop = safePadding.top + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val itemsPadding = PaddingValues(start = paddingStart, end = paddingEnd)

    Box(modifier = Modifier.fillMaxSize()) {
        state.person?.let { person ->
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(paddingTop))
                when {
                    windowSizeClass.isWidthAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    ) -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(itemsPadding),
                            horizontalArrangement =
                                Arrangement.spacedBy(MaterialTheme.spacings.default),
                        ) {
                            PersonImage(person)
                            Column(
                                verticalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.medium)
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                if (person.overview.isNotBlank()) {
                                    OverviewText(text = person.overview, maxCollapsedLines = 12)
                                }
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(itemsPadding),
                            verticalArrangement =
                                Arrangement.spacedBy(MaterialTheme.spacings.medium),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PersonImage(person)
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            if (person.overview.isNotBlank()) {
                                OverviewText(text = person.overview, maxCollapsedLines = 4)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacings.default))

                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default)) {
                    if (state.starredInMovies.isNotEmpty()) {
                        Column {
                            Text(
                                text = stringResource(CoreR.string.movies_label),
                                modifier = Modifier.padding(itemsPadding),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                            LazyRow(
                                contentPadding = itemsPadding,
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.default),
                            ) {
                                items(state.starredInMovies, key = { it.id }) { item ->
                                    ItemCard(
                                        item = item,
                                        direction = Direction.VERTICAL,
                                        onClick = {},
                                    )
                                }
                            }
                        }
                    }

                    if (state.starredInShows.isNotEmpty()) {
                        Column {
                            Text(
                                text = stringResource(CoreR.string.shows_label),
                                modifier = Modifier.padding(itemsPadding),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                            LazyRow(
                                contentPadding = itemsPadding,
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.default),
                            ) {
                                items(state.starredInShows, key = { it.id }) { item ->
                                    ItemCard(
                                        item = item,
                                        direction = Direction.VERTICAL,
                                        onClick = { onAction(PersonAction.NavigateToItem(item)) },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(paddingBottom))
            }
        } ?: run { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }

        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = MaterialTheme.spacings.small)
        ) {
            IconButton(
                onClick = { onAction(PersonAction.NavigateBack) },
                modifier = Modifier.alpha(0.7f),
                colors =
                    IconButtonDefaults.iconButtonColors(
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

@Composable
private fun PersonImage(person: FindroidPerson, modifier: Modifier = Modifier) {
    AsyncImage(
        model = person.images.primary,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .height(320.dp)
                .aspectRatio(0.66f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}

@PreviewScreenSizes
@Composable
private fun PersonScreenLayoutPreview() {
    FindroidTheme {
        PersonScreenLayout(
            state = PersonState(person = dummyPersonDetail, starredInMovies = dummyMovies),
            onAction = {},
        )
    }
}
