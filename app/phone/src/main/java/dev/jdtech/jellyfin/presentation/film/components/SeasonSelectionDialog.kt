package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummySeason
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.UUID

@Composable
fun SeasonSelectionDialog(
    seasons: List<FindroidSeason>,
    onConfirm: (selectedSeasonIds: Set<UUID>) -> Unit,
    onDismiss: () -> Unit,
    initialSelection: Set<UUID> = seasons.map { it.id }.toSet(),
) {
    val lazyListState = rememberLazyListState()

    var selectedSeasons by remember(initialSelection) { mutableStateOf(initialSelection) }

    BaseDialog(
        title = stringResource(CoreR.string.select_seasons),
        onDismiss = onDismiss,
        negativeButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(CoreR.string.cancel)) }
        },
        positiveButton = {
            TextButton(
                onClick = { onConfirm(selectedSeasons) },
                enabled = selectedSeasons.isNotEmpty(),
            ) {
                Text(text = stringResource(CoreR.string.download_button_description))
            }
        },
    ) {
        if (lazyListState.canScrollBackward) {
            HorizontalDivider()
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            state = lazyListState,
        ) {
            items(items = seasons, key = { it.id }) { season ->
                SeasonSelectionDialogItem(
                    season = season,
                    checked = selectedSeasons.contains(season.id),
                    onCheckedChange = { id ->
                        selectedSeasons =
                            if (selectedSeasons.contains(id)) {
                                selectedSeasons - id
                            } else {
                                selectedSeasons + id
                            }
                    },
                )
            }
        }
        if (lazyListState.canScrollForward) {
            HorizontalDivider()
        }
    }
}

@Composable
private fun SeasonSelectionDialogItem(
    season: FindroidSeason,
    checked: Boolean,
    onCheckedChange: (UUID) -> Unit,
) {
    val seasonLabel =
        if (season.name.isNotBlank()) {
            season.name
        } else {
            stringResource(CoreR.string.season_number, season.indexNumber)
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onCheckedChange(season.id) }
                .padding(horizontal = MaterialTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { _ -> onCheckedChange(season.id) })
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Text(text = seasonLabel, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview
@Composable
private fun SeasonSelectionDialogPreview() {
    FindroidTheme {
        SeasonSelectionDialog(seasons = listOf(dummySeason), onConfirm = {}, onDismiss = {})
    }
}
