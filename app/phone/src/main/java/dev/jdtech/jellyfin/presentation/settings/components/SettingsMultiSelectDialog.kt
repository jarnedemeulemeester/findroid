package dev.jdtech.jellyfin.presentation.settings.components

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceMultiSelect
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
fun SettingsMultiSelectDialog(
    preference: PreferenceMultiSelect,
    options: List<Pair<String?, String>>,
    onUpdate: (value: Set<String>?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val isAtTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    var selectedOptions by remember {
        mutableStateOf(preference.value?.toSet() ?: emptySet())
    }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacings.default),
            ) {
                Text(
                    text = stringResource(preference.nameStringResource),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                if (!isAtTop) {
                    HorizontalDivider()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = lazyListState,
                ) {
                    items(options) { option ->
                        SettingsMultiSelectDialogItem(
                            option = option,
                            checked = selectedOptions.contains(option.first),
                            onCheckedChange = { key ->
                                selectedOptions = if (selectedOptions.contains(key)) {
                                    selectedOptions - setOfNotNull(key)
                                } else {
                                    selectedOptions + listOfNotNull(key)
                                }
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text(text = stringResource(SettingsR.string.cancel))
                    }
                    TextButton(
                        onClick = { onUpdate(selectedOptions.ifEmpty { emptySet() }) },
                    ) {
                        Text(text = stringResource(SettingsR.string.save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMultiSelectDialogItem(
    option: Pair<String?, String>,
    checked: Boolean,
    onCheckedChange: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(option.first) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { _ -> onCheckedChange(option.first) },
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Text(text = option.second)
    }
}

@Preview
@Composable
private fun SettingsMultiSelectDialogPreview() {
    FindroidTheme {
        SettingsMultiSelectDialog(
            preference = PreferenceMultiSelect(
                nameStringResource = SettingsR.string.pref_player_media_segments_skip_button_type,
                backendPreference = Preference("", emptySet()),
                options = SettingsR.array.media_segments_type,
                optionValues = SettingsR.array.media_segments_type_values,
            ),
            options = listOf(
                "a" to "Option A",
                "b" to "Option B",
                "c" to "Option C",
            ),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
