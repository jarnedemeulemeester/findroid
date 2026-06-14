package dev.jdtech.jellyfin.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsFileEditAction
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsFileEditViewModel
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
fun SettingsFileEditScreen(
    filePath: String,
    navigateBack: () -> Unit,
    viewModel: SettingsFileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadFile(
            filePath = filePath,
        )
    }

    SettingsFileEditScreenLayout(
        title = SettingsR.string.mpv_edit_conf_file,
        onAction = { action ->
            when (action) {
                is SettingsFileEditAction.OnBackClick -> navigateBack()
                else -> Unit
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsFileEditScreenLayout(
    @StringRes title: Int,
    onAction: (SettingsFileEditAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                .recalculateWindowInsets()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsFileEditAction.OnBackClick) }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
            ) {
                Icon(
                    painterResource(CoreR.drawable.ic_save),
                    contentDescription = null,
                )
            }
        }
    ) { innerPadding ->
        TextField(
            state = rememberTextFieldState(),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
@PreviewScreenSizes
private fun SettingsFileEditScreenLayoutPreview() {
    FindroidTheme {
        SettingsFileEditScreenLayout(
            title = SettingsR.string.mpv_edit_conf_file,
            onAction = {},
        )
    }
}