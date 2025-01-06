package dev.jdtech.jellyfin.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.models.PreferenceCategory
import dev.jdtech.jellyfin.presentation.settings.components.SettingsCategoryCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsEvent
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsState
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsScreen(
    navigateToSubSettings: (indexes: IntArray, title: Int) -> Unit,
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadPreferences()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> navigateToSubSettings(event.indexes, event.title)
            is SettingsEvent.NavigateToUsers -> navigateToUsers()
            is SettingsEvent.NavigateToServers -> navigateToServers()
        }
    }

    SettingsScreenLayout(
        state = state,
    )
}

@Composable
private fun SettingsScreenLayout(
    state: SettingsState,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingTop = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    val paddingStart = safePaddingStart + MaterialTheme.spacings.default
    val paddingTop = safePaddingTop + MaterialTheme.spacings.default
    val paddingEnd = safePaddingEnd + MaterialTheme.spacings.default
    val paddingBottom = safePaddingBottom + MaterialTheme.spacings.default

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = paddingStart,
            top = paddingTop,
            end = paddingEnd,
            bottom = paddingBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(CoreR.string.title_settings),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        items(state.preferences) { preference ->
            when (preference) {
                is PreferenceCategory -> SettingsCategoryCard(
                    preference = preference,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        SettingsScreenLayout(
            state = SettingsState(
                preferences = listOf(
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_language,
                        iconDrawableId = CoreR.drawable.ic_languages,
                    ),
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_appearance,
                        iconDrawableId = CoreR.drawable.ic_palette,
                    ),
                ),
            ),
        )
    }
}
