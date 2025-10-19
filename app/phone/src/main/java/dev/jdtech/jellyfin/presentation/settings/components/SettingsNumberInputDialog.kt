package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceIntInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceLongInput
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

@Composable
fun SettingsIntInputDialog(
    preference: PreferenceIntInput,
    onUpdate: (value: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val suffix = preference.suffixRes?.let {
        stringResource(it)
    }

    SettingsNumberInputDialog(
        preference = preference,
        initialValue = preference.value.toString(),
        onUpdate = { value ->
            value.toIntOrNull()?.let { value ->
                onUpdate(value)
            }
        },
        onDismissRequest = onDismissRequest,
        suffix = suffix,
    )
}

@Composable
fun SettingsLongInputDialog(
    preference: PreferenceLongInput,
    onUpdate: (value: Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val suffix = preference.suffixRes?.let {
        stringResource(it)
    }

    SettingsNumberInputDialog(
        preference = preference,
        initialValue = preference.value.toString(),
        onUpdate = { value ->
            value.toLongOrNull()?.let { value ->
                onUpdate(value)
            }
        },
        onDismissRequest = onDismissRequest,
        suffix = suffix,
    )
}

@Composable
fun SettingsNumberInputDialog(
    preference: Preference,
    initialValue: String,
    onUpdate: (String) -> Unit,
    onDismissRequest: () -> Unit,
    suffix: String? = null,
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(initialValue.length),
            ),
        )
    }

    val focusRequester = remember { FocusRequester() }

    // Only digits pattern
    val pattern = remember { Regex("^\\d+\$") }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
        negativeButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(
                    text = stringResource(SettingsR.string.cancel),
                )
            }
        },
        positiveButton = {
            TextButton(
                onClick = { onUpdate(textFieldValue.text) },
            ) {
                Text(
                    text = stringResource(SettingsR.string.save),
                )
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(contentPadding),
        ) {
            preference.descriptionStringRes?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            }
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = {
                    if (it.text.isEmpty() || it.text.matches(pattern)) {
                        textFieldValue = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                suffix = {
                    suffix?.let {
                        Text(text = it)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onUpdate(textFieldValue.text)
                    },
                ),
                singleLine = true,
            )
        }
    }
}

@Preview
@Composable
private fun SettingsNumberInputDialogPreview() {
    JellyCastTheme {
        SettingsNumberInputDialog(
            preference = PreferenceIntInput(
                nameStringResource = SettingsR.string.settings_cache_size,
                descriptionStringRes = SettingsR.string.settings_cache_size_message,
                backendPreference = PreferenceBackend("", 0),
                suffixRes = SettingsR.string.mb,
            ),
            initialValue = "20",
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
