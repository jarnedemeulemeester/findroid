package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.collection.CollectionViewModel
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.SortByDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridCellsAdaptiveWithMinColumns
import dev.jdtech.jellyfin.presentation.utils.plus
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CollectionScreen(
    collectionId: UUID,
    collectionName: String,
    onePerGenre: Boolean = false,
    onItemClick: (item: FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var onePerGenreState by remember { mutableStateOf(onePerGenre) }

    LaunchedEffect(true) {
        viewModel.loadItems(
            collectionId,
            onePerGenreState,
        )
    }

    CollectionScreenLayout(
        collectionName = collectionName,
        state = state,
        onBack = { navigateBack() },
        onItemClick = { item -> onItemClick(item) },
        onGenreSelected = { genre -> viewModel.selectGenre(genre) },
        onePerGenreState = onePerGenreState,
        onToggleOnePerGenre = {
            onePerGenreState = !onePerGenreState
            viewModel.loadItems(collectionId, onePerGenreState)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreenLayout(
    collectionName: String,
    state: CollectionState,
    onBack: () -> Unit,
    onItemClick: (FindroidItem) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onePerGenreState: Boolean,
    onToggleOnePerGenre: () -> Unit,
) {
    val contentPadding = PaddingValues(
        all = MaterialTheme.spacings.default,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showSortByDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) },
        modifier = Modifier
            .fillMaxSize()
            .recalculateWindowInsets()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(collectionName)
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                onBack()
                            },
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    dev.jdtech.jellyfin.presentation.components.CastButton()
                    IconButton(onClick = { showSortByDialog = true }) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_arrow_down_up),
                            contentDescription = "Ordenar",
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column {
            LazyVerticalGrid(
                columns = GridCellsAdaptiveWithMinColumns(minSize = 160.dp, minColumns = 2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = if (state.genres.isNotEmpty()) {
                    // When genres exist, remove top padding to make carousel stick to top
                    PaddingValues(
                        start = MaterialTheme.spacings.default,
                        end = MaterialTheme.spacings.default,
                        bottom = MaterialTheme.spacings.default,
                    ) + innerPadding
                } else {
                    contentPadding + innerPadding
                },
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            ) {
                // Genre carousel (shown once at the top, before all sections)
                if (state.genres.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = MaterialTheme.spacings.small),
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.medium),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                        ) {
                            // "Todos" chip
                            item {
                                FilterChip(
                                    selected = state.selectedGenre == null,
                                    onClick = { onGenreSelected(null) },
                                    label = { Text("Todos") },
                                )
                            }
                            // Genre chips
                            items(state.genres) { genre ->
                                FilterChip(
                                    selected = genre == state.selectedGenre,
                                    onClick = { onGenreSelected(genre) },
                                    label = { Text(genre) },
                                )
                            }
                        }
                    }
                }
                
                // Show all items from all sections without headers
                state.sections.forEach { section ->
                    items(
                        items = section.items,
                        key = { it.id },
                    ) { item ->
                        ItemCard(
                            item = item,
                            direction = if (item is FindroidEpisode) Direction.HORIZONTAL else Direction.VERTICAL,
                            onClick = {
                                onItemClick(item)
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            // Sort/Filter dialog (includes genre picker)
            if (showSortByDialog) {
                SortByDialog(
                    currentSortBy = SortBy.NAME,
                    currentSortOrder = org.jellyfin.sdk.model.api.SortOrder.ASCENDING,
                    onUpdate = { _, _ -> /* collection sorting not wired here */ },
                    onDismissRequest = { showSortByDialog = false },
                    genres = state.genres,
                    currentGenre = state.selectedGenre,
                    onGenreSelected = { genre ->
                        onGenreSelected(genre)
                        showSortByDialog = false
                    },
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun CollectionScreenLayoutPreview() {
    FindroidTheme {
        CollectionScreenLayout(
            collectionName = "Marvel",
            state = CollectionState(sections = listOf(CollectionSection(id = 0, name = UiText.StringResource(CoreR.string.movies_label), items = dummyMovies))),
            onBack = {},
            onItemClick = {},
            onGenreSelected = {},
            onePerGenreState = false,
            onToggleOnePerGenre = {}
        )
    }
}
