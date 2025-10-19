package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceButton
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsButtonCard(
    preference: PreferenceButton,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(enabled = preference.enabled) {
                preference.onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        preference.iconDrawableId?.let { iconId ->
            Icon(
                painter = painterResource(iconId),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(preference.nameStringResource),
                style = MaterialTheme.typography.bodyLarge,
                color = if (preference.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            // Show dynamic description if available, otherwise use resource string
            val dynamicDesc = preference.descriptionString
            if (dynamicDesc != null) {
                Text(
                    text = dynamicDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (preference.enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                )
            } else {
                preference.descriptionStringRes?.let { descId ->
                    Text(
                        text = stringResource(descId),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (preference.enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                    )
                }
            }
        }
        
        Icon(
            painter = painterResource(CoreR.drawable.ic_arrow_right),
            contentDescription = null,
            tint = if (preference.enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            },
        )
    }
}
