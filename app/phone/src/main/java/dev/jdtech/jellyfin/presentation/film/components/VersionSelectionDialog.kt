package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun VersionSelectionDialog(
    mediaSources: List<Pair<String, String>>,
    onSelect: (mediaSourceId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        title = stringResource(CoreR.string.select_video_version_title),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = MaterialTheme.spacings.default)
        ) {
            mediaSources.forEachIndexed { i, mediaSource ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(mediaSource.first) }
                            .height(48.dp)
                            .padding(horizontal = MaterialTheme.spacings.default),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = mediaSource.second, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
@Preview
private fun VersionSelectionDialogDialogPreview() {
    FindroidTheme {
        VersionSelectionDialog(
            mediaSources = listOf("A" to "Option A", "B" to "Option B", "C" to "Option C"),
            onSelect = {},
            onDismiss = {},
        )
    }
}
