package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceAction

@Composable
fun SettingsActionCard(
    preference: PreferenceAction,
    modifier: Modifier = Modifier,
    onFocusChange: ((androidx.compose.ui.focus.FocusState) -> Unit)? = null,
) {
    Surface(
        onClick = preference.onClick,
        enabled = preference.enabled,
        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.then(onFocusChange?.let { Modifier.onFocusChanged(it) } ?: Modifier),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            preference.iconDrawableId?.let {
                androidx.tv.material3.Icon(painter = painterResource(it), contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
            }
            Column {
                Text(stringResource(preference.nameStringResource), style = MaterialTheme.typography.titleMedium)
                preference.descriptionStringRes?.let {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
