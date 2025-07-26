package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmSearchBar(
    modifier: Modifier = Modifier,
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    inputPaddingStart: Dp = 0.dp,
    inputPaddingEnd: Dp = 0.dp,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val searchBarPaddingStart by animateDpAsState(
        targetValue = if (expanded) 0.dp else paddingStart,
        label = "search_bar_padding_start",
    )

    val searchBarPaddingEnd by animateDpAsState(
        targetValue = if (expanded) 0.dp else paddingEnd,
        label = "search_bar_padding_end",
    )

    val searchBarInputPaddingStart by animateDpAsState(
        targetValue = if (expanded) inputPaddingStart else 0.dp,
        label = "search_bar_padding_start",
    )

    val searchBarInputPaddingEnd by animateDpAsState(
        targetValue = if (expanded) inputPaddingEnd else 0.dp,
        label = "search_bar_padding_end",
    )

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { expanded = true },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .padding(start = searchBarInputPaddingStart, end = searchBarInputPaddingEnd),
                placeholder = {
                    Text(
                        text = stringResource(FilmR.string.search_placeholder),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                },
                leadingIcon = {
                    AnimatedContent(
                        targetState = expanded,
                        label = "search_to_back",
                    ) { targetExpanded ->
                        if (targetExpanded) {
                            IconButton(
                                onClick = {
                                    expanded = false
                                },
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
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                            },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_x),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .padding(
                start = searchBarPaddingStart,
                end = searchBarPaddingEnd,
            ),
    ) { }
}
