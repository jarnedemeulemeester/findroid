package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.ui.components.SettingsCard
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
) {
    SettingsScreenLayout()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsScreenLayout() {
    val focusRequester = remember { FocusRequester() }

    val settings = mapOf(
        "Language" to CoreR.drawable.ic_languages,
        "Appearance" to CoreR.drawable.ic_palette,
        "Player" to CoreR.drawable.ic_play,
        "Users" to CoreR.drawable.ic_user,
        "Servers" to CoreR.drawable.ic_server,
    ).toList()

    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2, vertical = MaterialTheme.spacings.large),
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF001721))))
            .focusRequester(focusRequester),
    ) {
        item(span = { TvGridItemSpan(this.maxLineSpan) }) {
            Text(
                text = stringResource(id = CoreR.string.title_settings),
                style = MaterialTheme.typography.displayMedium,
            )
        }
        items(settings) { setting ->
            SettingsCard(name = setting.first, icon = setting.second, onClick = {})
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            SettingsScreenLayout()
        }
    }
}
