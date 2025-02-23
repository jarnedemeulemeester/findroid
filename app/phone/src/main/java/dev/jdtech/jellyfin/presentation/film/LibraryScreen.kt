package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction
import dev.jdtech.jellyfin.film.presentation.library.LibraryState
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.SortByDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun LibraryScreen(
    libraryId: UUID,
    libraryName: String,
    libraryType: CollectionType,
    navigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var sortBy by rememberSaveable {
        mutableStateOf(SortBy.NAME)
    }
    var sortOrder by rememberSaveable {
        mutableStateOf(SortOrder.ASCENDING)
    }

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadItems(
            parentId = libraryId,
            libraryType = libraryType,
            sortBy = sortBy,
            sortOrder = sortOrder,
        )
    }

    LibraryScreenLayout(
        libraryName = libraryName,
        sortBy = sortBy,
        sortOrder = sortOrder,
        state = state,
        onAction = { action ->
            when (action) {
                is LibraryAction.OnBackClick -> navigateBack()
                is LibraryAction.ChangeSorting -> {
                    sortBy = action.sortBy
                    sortOrder = action.sortOrder
                }
                else -> Unit
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenLayout(
    libraryName: String,
    sortBy: SortBy,
    sortOrder: SortOrder,
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    val paddingStart = safePaddingStart + MaterialTheme.spacings.default
    val paddingTop = MaterialTheme.spacings.default
    val paddingEnd = safePaddingEnd + MaterialTheme.spacings.default
    val paddingBottom = safePaddingBottom + MaterialTheme.spacings.default

    val items = state.items.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showSortByDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingStart + innerPadding.calculateStartPadding(layoutDirection),
                top = paddingTop + innerPadding.calculateTopPadding(),
                end = paddingEnd + innerPadding.calculateEndPadding(layoutDirection),
                bottom = paddingBottom + innerPadding.calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
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

    if (showSortByDialog) {
        SortByDialog(
            currentSortBy = sortBy,
            currentSortOrder = sortOrder,
            onUpdate = { sortBy, sortOrder ->
                onAction(LibraryAction.ChangeSorting(sortBy, sortOrder))
            },
            onDismissRequest = {
                showSortByDialog = false
            },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryScreenLayoutPreview() {
    FindroidTheme {
        LibraryScreenLayout(
            libraryName = "Movies",
            sortBy = SortBy.NAME,
            sortOrder = SortOrder.ASCENDING,
            state = LibraryState(),
            onAction = {},
        )
    }
}
