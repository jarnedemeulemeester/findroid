package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    onUpdate: (value: String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val optionNames = stringArrayResource(preference.options)
    val optionValues = stringArrayResource(preference.optionValues)

    val options = optionNames.zip(optionValues)

    val lazyListState = rememberLazyListState()

    val isAtTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
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
                if (!isAtTop) {
                    HorizontalDivider()
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    state = lazyListState,
                ) {
                    items(options) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdate(option.second)
                                }
                                .padding(
                                    horizontal = MaterialTheme.spacings.default,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = option.second == preference.value,
                                onClick = {
                                    onUpdate(option.second)
                                },
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
                            Text(
                                text = option.first,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
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
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
