package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.jdtech.jellyfin.Preference
import dev.jdtech.jellyfin.PreferenceType
import dev.jdtech.jellyfin.destinations.ServerSelectScreenDestination
import dev.jdtech.jellyfin.destinations.UserSelectScreenDestination
import dev.jdtech.jellyfin.ui.components.SettingsCard
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
) {
    val preferences = listOf(
        Preference(
            nameStringResource = CoreR.string.settings_category_language,
            iconDrawableId = CoreR.drawable.ic_languages,
            type = PreferenceType.CATEGORY,
        ),
        Preference(
            nameStringResource = CoreR.string.settings_category_appearance,
            iconDrawableId = CoreR.drawable.ic_palette,
            type = PreferenceType.CATEGORY,
        ),
        Preference(
            nameStringResource = CoreR.string.settings_category_player,
            iconDrawableId = CoreR.drawable.ic_play,
            type = PreferenceType.CATEGORY,
        ),
        Preference(
            nameStringResource = CoreR.string.users,
            iconDrawableId = CoreR.drawable.ic_user,
            type = PreferenceType.NAVIGATE,
            onClick = {
                navigator.navigate(UserSelectScreenDestination)
            },
        ),
        Preference(
            nameStringResource = CoreR.string.settings_category_servers,
            iconDrawableId = CoreR.drawable.ic_server,
            type = PreferenceType.NAVIGATE,
            onClick = {
                navigator.navigate(ServerSelectScreenDestination)
            },
        ),
        Preference(
            nameStringResource = CoreR.string.settings_category_device,
            iconDrawableId = CoreR.drawable.ic_smartphone,
            type = PreferenceType.CATEGORY,
        ),
        Preference(
            nameStringResource = CoreR.string.settings_category_network,
            iconDrawableId = CoreR.drawable.ic_smartphone,
            type = PreferenceType.CATEGORY,
        ),
        Preference(
            nameStringResource = CoreR.string.settings_category_cache,
            iconDrawableId = null,
            type = PreferenceType.CATEGORY,
        ),
        Preference(
            nameStringResource = CoreR.string.about,
            iconDrawableId = null,
            type = PreferenceType.CATEGORY,
        ),
    )

    SettingsScreenLayout(preferences)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsScreenLayout(
    preferences: List<Preference>,
) {
    val focusRequester = remember { FocusRequester() }

    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(3),
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
        items(preferences) { preference ->
            SettingsCard(preference)
        }
    }
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            SettingsScreenLayout(
                listOf(
                    Preference(
                        nameStringResource = CoreR.string.settings_category_language,
                        iconDrawableId = CoreR.drawable.ic_languages,
                        type = PreferenceType.CATEGORY,
                    ),
                    Preference(
                        nameStringResource = CoreR.string.settings_category_appearance,
                        iconDrawableId = CoreR.drawable.ic_palette,
                        type = PreferenceType.CATEGORY,
                    ),
                ),
            )
        }
    }
}
