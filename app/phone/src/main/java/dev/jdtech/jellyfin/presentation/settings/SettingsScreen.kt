package dev.jdtech.jellyfin.presentation.settings

import android.app.UiModeManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.presentation.settings.components.SettingsGroupCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsAction
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsEvent
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsState
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
fun SettingsScreen(
    indexes: IntArray = intArrayOf(),
    navigateToSettings: (indexes: IntArray) -> Unit,
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    navigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadPreferences(indexes)
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> navigateToSettings(event.indexes)
            is SettingsEvent.NavigateToUsers -> navigateToUsers()
            is SettingsEvent.NavigateToServers -> navigateToServers()
            is SettingsEvent.UpdateTheme -> {
                val uiModeManager = context.getSystemService(UiModeManager::class.java)
                val nightMode = when (event.theme) {
                    "system" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) UiModeManager.MODE_NIGHT_AUTO else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    "light" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) UiModeManager.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) UiModeManager.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_YES
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) UiModeManager.MODE_NIGHT_AUTO else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    uiModeManager.setApplicationNightMode(nightMode)
                } else {
                    AppCompatDelegate.setDefaultNightMode(nightMode)
                }
            }
        }
    }

    SettingsScreenLayout(
        title = indexes.last(),
        state = state,
        onAction = { action ->
            when (action) {
                is SettingsAction.OnBackClick -> navigateBack()
                is SettingsAction.OnUpdate -> {
                    viewModel.onAction(action)
                    viewModel.loadPreferences(indexes)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenLayout(
    @StringRes title: Int,
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    val paddingStart = safePaddingStart + MaterialTheme.spacings.default
    val paddingTop = MaterialTheme.spacings.default
    val paddingEnd = safePaddingEnd + MaterialTheme.spacings.default
    val paddingBottom = safePaddingBottom + MaterialTheme.spacings.default

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(title))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(SettingsAction.OnBackClick)
                        },
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = paddingStart + innerPadding.calculateStartPadding(layoutDirection),
                top = paddingTop + innerPadding.calculateTopPadding(),
                end = paddingEnd + innerPadding.calculateEndPadding(layoutDirection),
                bottom = paddingBottom + innerPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(state.preferenceGroups) { group ->
                SettingsGroupCard(
                    group = group,
                    onAction = onAction,
                    modifier = Modifier
                        .widthIn(max = 640.dp),
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
            title = CoreR.string.title_settings,
            state = SettingsState(
                preferenceGroups = listOf(
                    PreferenceGroup(
                        nameStringResource = null,
                        preferences = listOf(
                            PreferenceCategory(
                                nameStringResource = SettingsR.string.settings_category_language,
                                iconDrawableId = SettingsR.drawable.ic_languages,
                            ),
                        ),
                    ),
                    PreferenceGroup(
                        nameStringResource = null,
                        preferences = listOf(
                            PreferenceCategory(
                                nameStringResource = SettingsR.string.settings_category_appearance,
                                iconDrawableId = SettingsR.drawable.ic_palette,
                            ),
                        ),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
