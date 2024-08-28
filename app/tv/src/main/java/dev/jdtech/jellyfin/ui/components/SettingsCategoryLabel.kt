package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.models.PreferenceCategoryLabel
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsCategoryLabel(
    preference: PreferenceCategoryLabel,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = preference.nameStringResource),
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier.fillMaxWidth()
    )
}

@Preview
@Composable
private fun SettingsLabelPreview() {
    FindroidTheme {
        SettingsCategoryLabel(
            preference = PreferenceCategoryLabel(nameStringResource = CoreR.string.mpv_player)
        )
    }
}
