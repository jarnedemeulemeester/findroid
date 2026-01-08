package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect

@Composable
fun SettingsSelectDialog(
    preference: PreferenceSelect,
    options: List<Pair<String?, String>>,
    onUpdate: (value: String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
    ) {
        if (lazyListState.canScrollBackward) {
            HorizontalDivider()
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            contentPadding = PaddingValues(bottom = MaterialTheme.spacings.default),
        ) {
            items(items = options, key = { it.first ?: "null" }) { option ->
                SettingsSelectDialogItem(
                    option = option,
                    isSelected = option.first == preference.value,
                    onSelect = onUpdate,
                )
            }
        }
    }
}

@Composable
private fun SettingsSelectDialogItem(
    option: Pair<String?, String>,
    isSelected: Boolean,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onSelect(option.first) }
                .padding(horizontal = MaterialTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = { onSelect(option.first) })
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Text(text = option.second, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview
@Composable
private fun SettingsSelectDialogPreview() {
    FindroidTheme {
        SettingsSelectDialog(
            preference =
                PreferenceSelect(
                    nameStringResource = SettingsR.string.settings_preferred_audio_language,
                    iconDrawableId = CoreR.drawable.ic_speaker,
                    backendPreference = Preference("", ""),
                    options = SettingsR.array.languages,
                    optionValues = SettingsR.array.languages_values,
                ),
            options = listOf("a" to "Option A", "b" to "Option B", "c" to "Option C"),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
