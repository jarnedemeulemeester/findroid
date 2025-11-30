package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceTextInput
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

@Composable
fun SettingsTextInputDialog(
    preference: PreferenceTextInput,
    onUpdate: (value: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = preference.value,
                selection = TextRange(preference.value.length),
            ),
        )
    }

    var passwordVisible by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

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
                    textFieldValue = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    preference.hintRes?.let {
                        Text(text = stringResource(it))
                    }
                },
                visualTransformation = if (preference.isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = if (preference.isPassword) {
                    {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (passwordVisible) CoreR.drawable.ic_eye_off else CoreR.drawable.ic_eye,
                                ),
                                contentDescription = null,
                            )
                        }
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (preference.isPassword) KeyboardType.Password else KeyboardType.Text,
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
private fun SettingsTextInputDialogPreview() {
    FindroidTheme {
        SettingsTextInputDialog(
            preference = PreferenceTextInput(
                nameStringResource = SettingsR.string.settings_proxy_host,
                hintRes = SettingsR.string.settings_proxy_host_hint,
                backendPreference = PreferenceBackend("", ""),
                value = "",
            ),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun SettingsTextInputDialogPasswordPreview() {
    FindroidTheme {
        SettingsTextInputDialog(
            preference = PreferenceTextInput(
                nameStringResource = SettingsR.string.settings_proxy_password,
                backendPreference = PreferenceBackend("", ""),
                isPassword = true,
                value = "secret",
            ),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
