package dev.jdtech.jellyfin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.core.presentation.dummy.dummyUser
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.presentation.film.HomeScreen
import dev.jdtech.jellyfin.presentation.film.MediaScreen
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.LoadingIndicator
import dev.jdtech.jellyfin.ui.components.PillBorderIndicator
import dev.jdtech.jellyfin.ui.components.ProfileButton
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun MainScreen(
    navigateToSettings: () -> Unit,
    navigateToLibrary: (libraryId: UUID, libraryName: String, libraryType: CollectionType) -> Unit,
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    navigateToPlayer: (itemId: UUID, itemKind: BaseItemKind) -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val delegatedUiState by mainViewModel.uiState.collectAsState()

    LaunchedEffect(true) {
        mainViewModel.loadServerAndUser()
    }

    MainScreenLayout(
        uiState = delegatedUiState,
        navigateToSettings = navigateToSettings,
        navigateToLibrary = navigateToLibrary,
        navigateToMovie = navigateToMovie,
        navigateToShow = navigateToShow,
        navigateToPlayer = navigateToPlayer,
    )
}

enum class TabDestination(
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    Search(CoreR.drawable.ic_search, CoreR.string.search),
    Home(CoreR.drawable.ic_home, CoreR.string.title_home),
    Libraries(CoreR.drawable.ic_library, CoreR.string.libraries),
    // LiveTV(CoreR.drawable.ic_tv, CoreR.string.live_tv)
}

@Composable
private fun MainScreenLayout(
    uiState: MainViewModel.UiState,
    navigateToSettings: () -> Unit,
    navigateToLibrary: (libraryId: UUID, libraryName: String, libraryType: CollectionType) -> Unit,
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    navigateToPlayer: (itemId: UUID, itemKind: BaseItemKind) -> Unit,
) {
    var focusedTabIndex by rememberSaveable { mutableIntStateOf(1) }
    var activeTabIndex by rememberSaveable { mutableIntStateOf(focusedTabIndex) }

    var isLoading by remember { mutableStateOf(false) }

    var user: User? = null
    when (uiState) {
        is MainViewModel.UiState.Normal -> {
            user = uiState.user
        }
        else -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = MaterialTheme.spacings.default),
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterStart),
            )
            TabRow(
                selectedTabIndex = focusedTabIndex,
                indicator = { tabPositions, isActivated ->
                    // FocusedTab's indicator
                    PillBorderIndicator(
                        currentTabPosition = tabPositions[focusedTabIndex],
                        activeBorderColor = Color.White,
                        inactiveBorderColor = Color.Transparent,
                        doesTabRowHaveFocus = isActivated,
                    )

                    // SelectedTab's indicator
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = tabPositions[activeTabIndex],
                        activeColor = Color.White,
                        inactiveColor = Color.White,
                        doesTabRowHaveFocus = isActivated,
                    )
                },
                modifier = Modifier.align(Alignment.Center),
            ) {
                TabDestination.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = activeTabIndex == index,
                        onFocus = { focusedTabIndex = index },
                        colors = TabDefaults.pillIndicatorTabColors(
                            contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            focusedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            focusedSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        onClick = {
                            focusedTabIndex = index
                            activeTabIndex = index
                        },
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacings.default / 2, vertical = MaterialTheme.spacings.small),
                    ) {
                        Icon(
                            painter = painterResource(id = tab.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.extraSmall))
                        Text(
                            text = stringResource(id = tab.label),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                if (isLoading) {
                    LoadingIndicator()
                }
                ProfileButton(
                    user = user,
                    onClick = {
                        navigateToSettings()
                    },
                )
            }
        }
        when (activeTabIndex) {
            1 -> {
                HomeScreen(
                    navigateToMovie = navigateToMovie,
                    navigateToShow = navigateToShow,
                    navigateToPlayer = navigateToPlayer,
                    isLoading = { isLoading = it },
                )
            }
            2 -> {
                MediaScreen(
                    navigateToLibrary = navigateToLibrary,
                    isLoading = { isLoading = it },
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun MainScreenLayoutPreview() {
    JellyCastTheme {
        MainScreenLayout(
            uiState = MainViewModel.UiState.Normal(server = dummyServer, user = dummyUser),
            navigateToSettings = {},
            navigateToLibrary = { _, _, _ -> },
            navigateToMovie = {},
            navigateToShow = {},
            navigateToPlayer = { _, _ -> },
        )
    }
}
