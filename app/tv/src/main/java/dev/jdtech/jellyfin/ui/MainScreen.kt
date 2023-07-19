package dev.jdtech.jellyfin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import dev.jdtech.jellyfin.ui.components.ProfileButton
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@RootNavGraph(start = true)
@Destination
@Composable
fun MainScreen(
    navigator: DestinationsNavigator,
) {
    MainScreenLayout(navigator)
}

enum class TabDestination(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
) {
    Search(CoreR.drawable.ic_search, CoreR.string.search),
    Home(CoreR.drawable.ic_home, CoreR.string.title_home),
    Libraries(CoreR.drawable.ic_library, CoreR.string.libraries),
    // LiveTV(CoreR.drawable.ic_tv, CoreR.string.live_tv)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MainScreenLayout(navigator: DestinationsNavigator) {
    var focusedTabIndex by remember { mutableIntStateOf(1) }
    var activeTabIndex by remember { mutableIntStateOf(focusedTabIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF001721)))),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 24.dp),
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterStart),
            )
            TabRow(
                selectedTabIndex = focusedTabIndex,
                indicator = { tabPositions ->
                    // FocusedTab's indicator
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = tabPositions[focusedTabIndex],
                        activeColor = Color.White,
                        inactiveColor = Color.Transparent,
                    )

                    // SelectedTab's indicator
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = tabPositions[activeTabIndex],
                        activeColor = Color.White,
                        inactiveColor = Color.White,
                    )
                },
                modifier = Modifier.align(Alignment.Center),
            ) {
                TabDestination.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = activeTabIndex == index,
                        onFocus = { focusedTabIndex = index },
                        colors = TabDefaults.pillIndicatorTabColors(
                            activeContentColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        onClick = {
                            focusedTabIndex = index
                            activeTabIndex = index
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = tab.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = tab.label),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
            ProfileButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.CenterEnd),
            )
        }
        when (activeTabIndex) {
            1 -> {
                HomeScreen(navigator = navigator)
            }
            2 -> {
                LibrariesScreen(navigator = navigator)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun MainScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            MainScreenLayout(navigator = EmptyDestinationsNavigator)
        }
    }
}
