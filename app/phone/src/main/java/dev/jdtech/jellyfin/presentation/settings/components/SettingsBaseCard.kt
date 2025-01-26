package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.models.Preference

@Composable
fun SettingsBaseCard(
    preference: Preference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val contentColor = contentColorFor(MaterialTheme.colorScheme.surface).copy(alpha = if (preference.enabled) 1.0f else 0.38f)
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = preference.enabled,
        contentColor = contentColor,
    ) {
        content()
    }
}
