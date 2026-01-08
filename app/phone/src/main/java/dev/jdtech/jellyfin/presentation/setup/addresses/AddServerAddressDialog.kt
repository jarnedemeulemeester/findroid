package dev.jdtech.jellyfin.presentation.setup.addresses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.setup.R as SetupR

@Composable
fun AddServerAddressDialog(onAdd: (address: String) -> Unit, onDismiss: () -> Unit) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = "")) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(true) { focusRequester.requestFocus() }

    BaseDialog(
        title = stringResource(SetupR.string.add_server_address),
        onDismiss = onDismiss,
        negativeButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(SetupR.string.cancel)) }
        },
        positiveButton = {
            TextButton(
                onClick = { onAdd(textFieldValue.text) },
                enabled = textFieldValue.text.isNotBlank(),
            ) {
                Text(text = stringResource(CoreR.string.add))
            }
        },
    ) { contentPadding ->
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier =
                Modifier.padding(contentPadding).fillMaxWidth().focusRequester(focusRequester),
            placeholder = { Text("http://<server_ip>:8096") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            singleLine = true,
        )
    }
}

@Preview
@Composable
private fun AddServerAddressDialogPreview() {
    FindroidTheme { AddServerAddressDialog(onAdd = {}, onDismiss = {}) }
}
