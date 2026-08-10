package dev.jdtech.jellyfin.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsFileEditAction
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsFileEditViewModel

@Composable
fun SettingsFileEditScreen(
    filePath: String,
    navigateBack: () -> Unit,
    viewModel: SettingsFileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(filePath) {
        viewModel.loadFile(filePath = filePath)
    }

    SettingsFileEditScreenLayout(
        fileName = filePath.split("/").last(),
        initialText = state.initialText,
        onAction = { action ->
            when (action) {
                is SettingsFileEditAction.OnBackClick -> navigateBack()
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsFileEditScreenLayout(
    fileName: String,
    initialText: String,
    onAction: (SettingsFileEditAction) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val isModified = initialText != textFieldState.text

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialText) {
        textFieldState.setTextAndPlaceCursorAtEnd(initialText)
        focusRequester.requestFocus()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    BackHandler(isModified) {
        showDiscardDialog = true
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(SettingsR.string.edit_file_title, fileName)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isModified) showDiscardDialog = true
                            else onAction(SettingsFileEditAction.OnBackClick)
                        }
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = textFieldState.text.toString()
                            onAction(SettingsFileEditAction.OnSave(text))
                        },
                        enabled = isModified,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_save),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        TextField(
            state = textFieldState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .focusRequester(focusRequester),
            textStyle =
                TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(SettingsR.string.discard_changes_dialog_title)) },
            text = { Text(stringResource(SettingsR.string.discard_changes_dialog_text)) },
            confirmButton = {
                TextButton(onClick = { onAction(SettingsFileEditAction.OnBackClick) }) {
                    Text(stringResource(CoreR.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(CoreR.string.cancel))
                }
            },
        )
    }
}

@Composable
@PreviewScreenSizes
private fun SettingsFileEditScreenLayoutPreview() {
    FindroidTheme {
        SettingsFileEditScreenLayout(
            fileName = "mpv.conf",
            initialText = "sample text",
            onAction = {},
        )
    }
}
