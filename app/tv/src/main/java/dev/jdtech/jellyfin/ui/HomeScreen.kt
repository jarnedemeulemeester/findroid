package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.viewmodels.HomeViewModel

@Destination
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = true) {
        homeViewModel.loadData(includeLibraries = true)
    }

    val delegatedUiState by homeViewModel.uiState.collectAsState()
    when (val uiState = delegatedUiState) {
        is HomeViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
        is HomeViewModel.UiState.Normal -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.homeItems, key = { it.id }) { homeItem ->
                    when (homeItem) {
                        is HomeItem.Libraries -> {
                            Text(text = homeItem.section.name)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                items(homeItem.section.items) { library ->
                                    Column(
                                        modifier = Modifier.width(320.dp).clickable {  }
                                    ) {
                                        Box(modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.77f)
                                            .background(MaterialTheme.colorScheme.surface))
                                        Text(text = library.name.orEmpty())
                                    }
                                }
                            }
                        }
                        is HomeItem.Section -> {
                            Text(text = homeItem.homeSection.name)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                items(homeItem.homeSection.items) { item ->
                                    Column(
                                        modifier = Modifier.width(320.dp).clickable {  }
                                    ) {
                                        Box(modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.77f)
                                            .background(MaterialTheme.colorScheme.surface))
                                        Text(text = item.name.orEmpty())
                                    }
                                }
                            }
                        }
                        is HomeItem.ViewItem -> {
                            Text(text = homeItem.view.name.orEmpty())
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                items(homeItem.view.items.orEmpty()) { item ->
                                    Column(
                                        modifier = Modifier.width(150.dp).clickable {  }
                                    ) {
                                        Box(modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.66f)
                                            .background(MaterialTheme.colorScheme.surface))
                                        Text(text = item.name.orEmpty())
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        is HomeViewModel.UiState.Error -> {
            Text(text = uiState.error.toString())
        }
    }
}