package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsSelectDialog(
    preference: PreferenceSelect,
    onDismissRequest: () -> Unit,
) {
    val optionNames = stringArrayResource(preference.options)
    val optionValues = stringArrayResource(preference.optionValues)

    val options = optionNames.zip(optionValues)

    var selectedOption by remember {
        mutableStateOf(preference.value)
    }

    Dialog(
        onDismissRequest = { onDismissRequest() },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 540.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column {
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                Text(
                    text = stringResource(preference.nameStringResource),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacings.default),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    items(options) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedOption = option.second
                                }
                                .padding(
                                    horizontal = MaterialTheme.spacings.default,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = option.second == selectedOption,
                                onClick = {
                                    selectedOption = option.second
                                },
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
                            Text(
                                text = option.first,
                            )
                        }
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MaterialTheme.spacings.medium,
                            top = MaterialTheme.spacings.extraSmall,
                            end = MaterialTheme.spacings.medium,
                            bottom = MaterialTheme.spacings.small,
                        ),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {},
                    ) {
                        Text("Save")
                    }
                    TextButton(
                        onClick = { onDismissRequest() },
                    ) {
                        Text(stringResource(CoreR.string.close))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ErrorDialogPreview() {
    FindroidTheme {
        SettingsSelectDialog(
            preference = PreferenceSelect(
                nameStringResource = CoreR.string.settings_preferred_audio_language,
                iconDrawableId = CoreR.drawable.ic_speaker,
                backendName = Constants.PREF_AUDIO_LANGUAGE,
                backendDefaultValue = null,
                options = CoreR.array.languages,
                optionValues = CoreR.array.languages_values,
            ),
            onDismissRequest = {},
        )
    }
}
