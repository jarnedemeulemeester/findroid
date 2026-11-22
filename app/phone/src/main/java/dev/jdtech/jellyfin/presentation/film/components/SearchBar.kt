package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.search.SearchAction
import dev.jdtech.jellyfin.film.presentation.search.SearchState
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridCellsAdaptiveWithMinColumns
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FilmSearchBar(
    state: SearchState,
    searchBarState: SearchBarState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
) {
    val scope = rememberCoroutineScope()

    val textFieldState = rememberTextFieldState()

    // The search bar expansion is owned by [searchBarState]; this is only used for the
    // collapsed bar's padding animation and the leading icon.
    val expanded = searchBarState.targetValue == SearchBarValue.Expanded

    val searchBarPaddingStart by
        animateDpAsState(
            targetValue = if (expanded) 0.dp else paddingStart,
            label = "search_bar_padding_start",
        )

    val searchBarPaddingEnd by
        animateDpAsState(
            targetValue = if (expanded) 0.dp else paddingEnd,
            label = "search_bar_padding_end",
        )

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { query ->
                if (query.isNotBlank()) {
                    // Debounce scales with input length. Max debounce of 300ms.
                    delay(minOf(50L + (query.count() * 50L), 300L).milliseconds)
                }
                onAction(SearchAction.Search(query))
            }
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {
                // Nothing to do: the search bar is already expanded while the query is being
                // edited, and results update live through the debounced search above.
            },
            placeholder = {
                Text(
                    text = stringResource(FilmR.string.search_placeholder),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            leadingIcon = {
                AnimatedContent(targetState = expanded, label = "search_to_back") { targetExpanded
                    ->
                    if (targetExpanded) {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_left),
                                contentDescription = null,
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_search),
                            contentDescription = null,
                        )
                    }
                }
            },
            trailingIcon = {
                if (state.loading) {
                    Box(modifier = Modifier.size(32.dp)) { CircularProgressIndicator() }
                } else if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.clearText() }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_x),
                            contentDescription = null,
                        )
                    }
                }
            },
        )
    }

    SearchBar(
        state = searchBarState,
        inputField = inputField,
        modifier =
            modifier
                .zIndex(1f) // Render the search bar above other content
                .windowInsetsPadding(SearchBarDefaults.windowInsets)
                .padding(start = searchBarPaddingStart, end = searchBarPaddingEnd),
    )

    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
        LazyVerticalGrid(
            columns = GridCellsAdaptiveWithMinColumns(minSize = 160.dp, minColumns = 2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(all = MaterialTheme.spacings.default),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
            items(items = state.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = { onAction(SearchAction.OnItemClick(item)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
