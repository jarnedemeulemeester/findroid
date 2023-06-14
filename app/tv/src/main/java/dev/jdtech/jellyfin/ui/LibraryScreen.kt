package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.material3.CompactCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import java.util.UUID

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination
@Composable
fun LibraryScreen(
    libraryId: UUID,
    libraryType: CollectionType,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        libraryViewModel.loadItems(libraryId, libraryType)
    }

    val api = JellyfinApi.getInstance(context)

    val delegatedUiState by libraryViewModel.uiState.collectAsState()
    when (val uiState = delegatedUiState) {
        is LibraryViewModel.UiState.Loading -> Text(text = "LOADING")
        is LibraryViewModel.UiState.Normal -> {
            val items = uiState.items.collectAsLazyPagingItems()
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp)
            ) {
                item(span = { TvGridItemSpan(this.maxLineSpan) }) {
                    Header()
                }
                items(items.itemCount) { i ->
                    val item = items[i]
                    item?.let {
                        CompactCard(
                            onClick = {  },
                            image = {
                                ItemPoster(
                                    item = item,
                                    api = api,
                                    direction = Direction.VERTICAL
                                )
                            },
                            title = {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp),
                                )
                            },
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }
        }
        is LibraryViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}
