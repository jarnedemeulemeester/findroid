package dev.jdtech.jellyfin.presentation.settings

import android.app.UiModeManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.recalculateWindowInsets
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.presentation.settings.components.SettingsGroupCard
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.plus
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsAction
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsEvent
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsState
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.restart
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
fun SettingsScreen(
    indexes: IntArray = intArrayOf(),
    navigateToSettings: (indexes: IntArray) -> Unit,
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Function to update external player description
    fun updatePlayerDescription() {
        val prefsName = context.packageName + "_preferences"
        val sharedPreferences = context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
        val selectedPlayerPackage = sharedPreferences.getString("pref_player_external_app", null)
        
        if (selectedPlayerPackage != null) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(selectedPlayerPackage, 0)
                val appName = context.packageManager.getApplicationLabel(appInfo).toString()
                viewModel.updateExternalPlayerDescription(appName)
            } catch (e: Exception) {
                // Player no longer installed
                viewModel.updateExternalPlayerDescription(null)
            }
        } else {
            viewModel.updateExternalPlayerDescription(null)
        }
    }

    LaunchedEffect(true) {
        viewModel.loadPreferences(indexes, DeviceType.PHONE)
        updatePlayerDescription()
    }
    
    // Update player description when screen becomes visible again (after returning from PlayerPickerActivity)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePlayerDescription()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> navigateToSettings(event.indexes)
            is SettingsEvent.NavigateToUsers -> navigateToUsers()
            is SettingsEvent.NavigateToServers -> navigateToServers()
            is SettingsEvent.NavigateToAbout -> navigateToAbout()
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
            is SettingsEvent.LaunchIntent -> {
                try {
                    context.startActivity(event.intent)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            is SettingsEvent.LaunchPlayerPicker -> {
                try {
                    Log.d("SettingsScreen", "LaunchPlayerPicker event triggered")
                    val intent = Intent(context, dev.jdtech.jellyfin.PlayerPickerActivity::class.java)
                    Log.d("SettingsScreen", "Starting PlayerPickerActivity")
                    context.startActivity(intent)
                    Log.d("SettingsScreen", "PlayerPickerActivity started successfully")
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Error starting PlayerPickerActivity", e)
                    Timber.e(e)
                }
            }
            is SettingsEvent.RestartApp -> {
                // Apply immediately so repository binding switches between online/offline
                (context as? android.app.Activity)?.restart()
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
                    viewModel.loadPreferences(indexes, DeviceType.PHONE)
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
    val contentPadding = PaddingValues(
        all = MaterialTheme.spacings.default,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .recalculateWindowInsets()
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
                actions = {
                    dev.jdtech.jellyfin.presentation.components.CastButton()
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = contentPadding + innerPadding,
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
    JellyCastTheme {
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
                                nameStringResource = SettingsR.string.settings_category_interface,
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
