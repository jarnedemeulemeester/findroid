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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction.SelectGenre
import dev.jdtech.jellyfin.film.presentation.library.LibraryState
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.ErrorDialog
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ErrorCard
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.SortByDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridCellsAdaptiveWithMinColumns
import dev.jdtech.jellyfin.presentation.utils.plus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun LibraryScreen(
    libraryId: UUID,
    libraryName: String,
    libraryType: CollectionType,
    onItemClick: (item: FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var initialLoad by rememberSaveable {
        mutableStateOf(true)
    }

    LaunchedEffect(true) {
        viewModel.setup(
            parentId = libraryId,
            libraryType = libraryType,
        )
        if (initialLoad) {
            viewModel.loadItems()
            initialLoad = false
        }
    }

    LibraryScreenLayout(
        libraryName = libraryName,
        state = state,
        onAction = { action ->
            when (action) {
                is LibraryAction.OnItemClick -> onItemClick(action.item)
                is LibraryAction.OnBackClick -> navigateBack()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenLayout(
    libraryName: String,
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
) {
    val contentPadding = PaddingValues(
        all = MaterialTheme.spacings.default,
    )

    val items = state.items.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showSortByDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .recalculateWindowInsets()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(libraryName)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(LibraryAction.OnBackClick)
                        },
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showSortByDialog = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_down_up),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column {
            ErrorGroup(
                loadStates = items.loadState,
                onRefresh = {
                    items.refresh()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding + innerPadding),
            )
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
                // Genre carousel at the top
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
                                    onClick = { onAction(SelectGenre(null)) },
                                    label = { Text("Todos") },
                                )
                            }
                            // Genre chips
                            items(state.genres) { genre ->
                                FilterChip(
                                    selected = genre == state.selectedGenre,
                                    onClick = { onAction(SelectGenre(genre)) },
                                    label = { Text(genre) },
                                )
                            }
                        }
                    }
                }
                
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.id },
                ) {
                    val item = items[it]
                    item?.let { item ->
                        ItemCard(
                            item = item,
                            direction = Direction.VERTICAL,
                            onClick = {
                                onAction(LibraryAction.OnItemClick(item))
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (showSortByDialog) {
        SortByDialog(
            currentSortBy = state.sortBy,
            currentSortOrder = state.sortOrder,
            onUpdate = { sortBy, sortOrder ->
                onAction(LibraryAction.ChangeSorting(sortBy, sortOrder))
            },
            onDismissRequest = {
                showSortByDialog = false
            },
            genres = state.genres,
            currentGenre = state.selectedGenre,
            onGenreSelected = { genre -> onAction(SelectGenre(genre)) },
        )
    }
}

@Composable
private fun ErrorGroup(loadStates: CombinedLoadStates, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }

    val loadStateError = when {
        loadStates.refresh is LoadState.Error -> {
            loadStates.refresh as LoadState.Error
        }
        loadStates.prepend is LoadState.Error -> {
            loadStates.prepend as LoadState.Error
        }
        loadStates.append is LoadState.Error -> {
            loadStates.append as LoadState.Error
        }
        else -> null
    }

    loadStateError?.let {
        ErrorCard(
            onShowStacktrace = {
                showErrorDialog = true
            },
            onRetryClick = onRefresh,
            modifier = modifier,
        )
        if (showErrorDialog) {
            ErrorDialog(
                exception = it.error,
                onDismissRequest = { showErrorDialog = false },
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryScreenLayoutPreview() {
    val items: Flow<PagingData<FindroidItem>> = flowOf(PagingData.from(dummyMovies))
    FindroidTheme {
        LibraryScreenLayout(
            libraryName = "Movies",
            state = LibraryState(items = items),
            onAction = {},
        )
    }
}
