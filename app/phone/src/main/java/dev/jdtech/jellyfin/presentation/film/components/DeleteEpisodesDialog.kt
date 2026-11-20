package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.UUID

@Composable
fun DeleteEpisodesDialog(
    episodes: List<FindroidEpisode>,
    isLoading: Boolean,
    onConfirm: (List<UUID>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIds = remember(episodes) { mutableStateListOf<UUID>() }
    val allSelected = episodes.isNotEmpty() && selectedIds.size == episodes.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                start = MaterialTheme.spacings.default,
                                end = MaterialTheme.spacings.default,
                                top = MaterialTheme.spacings.small,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(CoreR.string.delete_episodes_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(episodes.map { it.id })
                            }
                        },
                        enabled = episodes.isNotEmpty(),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    if (allSelected) CoreR.string.deselect_all
                                    else CoreR.string.select_all
                                )
                        )
                    }
                }
                Text(
                    text = stringResource(CoreR.string.delete_episodes_selected, selectedIds.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacings.default),
                )
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                HorizontalDivider()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        episodes.isEmpty() -> {
                            Text(
                                text = stringResource(CoreR.string.no_episodes),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        else -> {
                            val groupedEpisodes = remember(episodes) {
                                episodes.groupBy { it.parentIndexNumber }
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                groupedEpisodes.forEach { (seasonNumber, seasonEpisodes) ->
                                    item(key = "season-$seasonNumber") {
                                        Text(
                                            text =
                                                stringResource(
                                                    CoreR.string.season_number,
                                                    seasonNumber,
                                                ),
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier =
                                                Modifier.padding(
                                                    start = MaterialTheme.spacings.default,
                                                    end = MaterialTheme.spacings.default,
                                                    top = MaterialTheme.spacings.medium,
                                                    bottom = MaterialTheme.spacings.extraSmall,
                                                ),
                                        )
                                    }
                                    items(seasonEpisodes, key = { it.id }) { episode ->
                                        val checked = selectedIds.contains(episode.id)
                                        val toggle = {
                                            if (checked) {
                                                selectedIds.remove(episode.id)
                                            } else {
                                                selectedIds.add(episode.id)
                                            }
                                        }
                                        Row(
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .clickable { toggle() }
                                                    .padding(
                                                        horizontal =
                                                            MaterialTheme.spacings.default,
                                                        vertical = 4.dp,
                                                    ),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { toggle() },
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text =
                                                    stringResource(
                                                        CoreR.string.episode_name,
                                                        episode.indexNumber,
                                                        episode.name,
                                                    ),
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                start = MaterialTheme.spacings.default,
                                end = MaterialTheme.spacings.default,
                                top = MaterialTheme.spacings.small,
                                bottom = MaterialTheme.spacings.small,
                            ),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(CoreR.string.cancel))
                    }
                    Spacer(Modifier.width(MaterialTheme.spacings.small))
                    Button(
                        onClick = { onConfirm(selectedIds.toList()) },
                        enabled = selectedIds.isNotEmpty(),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    CoreR.string.delete_episodes_confirm,
                                    selectedIds.size,
                                )
                        )
                    }
                }
            }
        }
    }
}
