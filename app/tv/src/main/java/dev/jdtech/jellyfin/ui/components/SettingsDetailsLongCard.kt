package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.isDigitsOnly
import androidx.media3.common.C.DEFAULT_SEEK_BACK_INCREMENT_MS
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.models.PreferenceLong
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsDetailsLongCard(
    preference: PreferenceLong,
    modifier: Modifier = Modifier,
    onValueUpdate: (Long) -> Unit,
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacings.default,
                vertical = MaterialTheme.spacings.medium,
            ),
        ) {
            Text(
                text = stringResource(id = preference.nameStringResource),
                style = MaterialTheme.typography.headlineMedium
            )
            preference.descriptionStringRes?.let {
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                Text(text = stringResource(id = it), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
            Column(modifier = Modifier.padding(vertical = MaterialTheme.spacings.medium)) {
                var text by remember(preference.value) { mutableStateOf(preference.value.toString()) }
                OutlinedTextField(
                    value = text,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        if (it.isDigitsOnly()) {
                            text = it
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                Box {
                    Button(
                        onClick = {
                            onValueUpdate(text.toLongOrNull() ?: return@Button)
                        },
                        enabled = text.isNotEmpty(),
                        scale = ButtonScale.None,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(id = android.R.string.ok),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }

            }
        }
    }
}

@Preview
@Composable
private fun SettingsDetailLongCardPreview() {
    FindroidTheme {
        SettingsDetailsLongCard(
            preference = PreferenceLong(
                nameStringResource = CoreR.string.seek_back_increment,
                backendName = Constants.PREF_PLAYER_SEEK_BACK_INC,
                backendDefaultValue = DEFAULT_SEEK_BACK_INCREMENT_MS,
                value = DEFAULT_SEEK_BACK_INCREMENT_MS,
            ),
            onValueUpdate = {},
        )
    }
}
