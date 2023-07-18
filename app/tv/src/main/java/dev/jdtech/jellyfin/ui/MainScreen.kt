package dev.jdtech.jellyfin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 24.dp),
        ) {
            TabRow(
                selectedTabIndex = focusedTabIndex,
                indicator = { tabPositions ->
                    // FocusedTab's indicator
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = tabPositions[focusedTabIndex],
                        activeColor = Color.White.copy(alpha = 0.7f),
                        inactiveColor = Color.Transparent,
                    )

                    // SelectedTab's indicator
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = tabPositions[activeTabIndex],
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                TabDestination.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = activeTabIndex == index,
                        onFocus = { focusedTabIndex = index },
                        colors = TabDefaults.pillIndicatorTabColors(
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary
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
        }
        when (activeTabIndex) {
            0 -> {}
            1 -> {
                HomeScreen(navigator = navigator)
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
