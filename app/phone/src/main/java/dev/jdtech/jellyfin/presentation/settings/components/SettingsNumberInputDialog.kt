package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceIntInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceLongInput
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

@Composable
fun SettingsIntInputDialog(
    preference: PreferenceIntInput,
    onUpdate: (value: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SettingsNumberInputDialog(
        preference = preference,
        initialValue = preference.value.toString(),
        onUpdate = { value ->
            value.toIntOrNull()?.let { value ->
                onUpdate(value)
            }
        },
        onDismissRequest = onDismissRequest,
        suffix = preference.suffix,
    )
}

@Composable
fun SettingsLongInputDialog(
    preference: PreferenceLongInput,
    onUpdate: (value: Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SettingsNumberInputDialog(
        preference = preference,
        initialValue = preference.value.toString(),
        onUpdate = { value ->
            value.toLongOrNull()?.let { value ->
                onUpdate(value)
            }
        },
        onDismissRequest = onDismissRequest,
        suffix = preference.suffix,
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

    Dialog(
        onDismissRequest = { onDismissRequest() },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(MaterialTheme.spacings.default),
            ) {
                Text(
                    text = stringResource(preference.nameStringResource),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                preference.descriptionStringRes?.let {
                    Text(
                        text = stringResource(it),
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
                    modifier = Modifier.focusRequester(focusRequester),
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
}

@Preview
@Composable
private fun SettingsNumberInputDialogPreview() {
    FindroidTheme {
        SettingsNumberInputDialog(
            preference = PreferenceIntInput(
                nameStringResource = CoreR.string.settings_cache_size,
                descriptionStringRes = CoreR.string.settings_cache_size_message,
                backendPreference = PreferenceBackend("", 0),
                suffix = "MB",
            ),
            initialValue = "20",
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
