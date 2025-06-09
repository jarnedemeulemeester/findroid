package dev.jdtech.jellyfin.presentation.film

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSection
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSuggestions
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeView
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.home.HomeState
import dev.jdtech.jellyfin.film.presentation.home.HomeViewModel
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.ErrorDialog
import dev.jdtech.jellyfin.presentation.film.components.ErrorCard
import dev.jdtech.jellyfin.presentation.film.components.FilmSearchBar
import dev.jdtech.jellyfin.presentation.film.components.HomeCarousel
import dev.jdtech.jellyfin.presentation.film.components.HomeSection
import dev.jdtech.jellyfin.presentation.film.components.HomeView
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun HomeScreen(
    onLibraryClick: (library: FindroidCollection) -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadData()
    }

    HomeScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is HomeAction.OnItemClick -> onItemClick(action.item)
                is HomeAction.OnLibraryClick -> onLibraryClick(action.library)
                is HomeAction.OnSettingsClick -> onSettingsClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenLayout(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val itemsPadding = PaddingValues(
        start = paddingStart,
        end = paddingEnd,
    )

    val contentPaddingTop by animateDpAsState(
        targetValue = if (state.error != null) {
            safePadding.top + 144.dp
        } else {
            safePadding.top + 88.dp
        },
        label = "content_padding",
    )

    var showErrorDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true },
    ) {
        FilmSearchBar(
            onSettingsClick = {
                onAction(HomeAction.OnSettingsClick)
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { traversalIndex = 0f },
            paddingStart = paddingStart,
            paddingEnd = paddingEnd,
            inputPaddingStart = safePadding.start,
            inputPaddingEnd = safePadding.end,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { traversalIndex = 1f },
            contentPadding = PaddingValues(
                top = contentPaddingTop,
                bottom = paddingBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            state.suggestionsSection?.let { section ->
                item(key = section.id) {
                    HomeCarousel(
                        items = section.items,
                        itemsPadding = itemsPadding,
                        onAction = onAction,
                    )
                }
            }
            state.resumeSection?.let { section ->
                item(key = section.id) {
                    HomeSection(
                        section = section.homeSection,
                        itemsPadding = itemsPadding,
                        onAction = onAction,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            state.nextUpSection?.let { section ->
                item(key = section.id) {
                    HomeSection(
                        section = section.homeSection,
                        itemsPadding = itemsPadding,
                        onAction = onAction,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(state.views, key = { it.id }) { view ->
                HomeView(
                    view = view,
                    itemsPadding = itemsPadding,
                    onAction = onAction,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (state.error != null) {
            ErrorCard(
                onShowStacktrace = {
                    showErrorDialog = true
                },
                onRetryClick = {
                    onAction(HomeAction.OnRetryClick)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = paddingStart,
                        top = safePadding.top + 80.dp,
                        end = paddingEnd,
                    ),
            )
            if (showErrorDialog) {
                ErrorDialog(
                    exception = state.error!!,
                    onDismissRequest = { showErrorDialog = false },
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        HomeScreenLayout(
            state = HomeState(
                suggestionsSection = dummyHomeSuggestions,
                resumeSection = dummyHomeSection,
                views = listOf(dummyHomeView),
                error = Exception("Failed to load data"),
            ),
            onAction = {},
        )
    }
}
