package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import org.jellyfin.sdk.model.api.SortOrder
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SortByDialog(
    currentSortBy: SortBy,
    currentSortOrder: SortOrder,
    onUpdate: (sortBy: SortBy, sortOrder: SortOrder) -> Unit,
    onDismissRequest: () -> Unit,
    genres: List<String> = emptyList(),
    currentGenre: String? = null,
    onGenreSelected: (String?) -> Unit = {},
) {
    val optionValues = SortBy.entries
    val optionNames = stringArrayResource(CoreR.array.sort_by_options)
    val options = optionValues.zip(optionNames)

    val orderValues = SortOrder.entries
    val orderNames = stringArrayResource(CoreR.array.sort_order_options)
    val orderOptions = orderValues.zip(orderNames)

    var selectedOption by remember {
        mutableStateOf(currentSortBy)
    }
    var selectedOrder by remember {
        mutableStateOf(currentSortOrder)
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column {
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                Text(
                    text = stringResource(CoreR.string.sort_by),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacings.default),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacings.default)
                        .fillMaxWidth(),
                ) {
                    orderOptions.forEachIndexed { index, order ->
                        SegmentedButton(
                            selected = order.first == selectedOrder,
                            onClick = {
                                selectedOrder = order.first
                                onUpdate(selectedOption, selectedOrder)
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = orderOptions.size,
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                inactiveContainerColor = Color.Transparent,
                            ),
                            icon = {
                                AnimatedVisibility(
                                    visible = order.first == selectedOrder,
                                    enter = fadeIn(),
                                    exit = ExitTransition.None,
                                ) {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_check),
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = {
                                Text(order.second)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                if (!isAtTop) {
                    HorizontalDivider()
                }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                state = lazyListState,
                            ) {
                                items(options) { option ->
                                    SortByDialogItem(
                                        option = option,
                                        isSelected = option.first == selectedOption,
                                        onSelect = {
                                            selectedOption = option.first
                                            onUpdate(selectedOption, selectedOrder)
                                        },
                                    )
                                }

                                // Genre selection removed - now handled by genre carousel in main screen
                            }
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            }
        }
    }
}

@Composable
private fun SortByDialogItem(
    option: Pair<SortBy, String>,
    isSelected: Boolean,
    onSelect: (SortBy) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect(option.first)
            }
            .padding(
                horizontal = MaterialTheme.spacings.default,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                onSelect(option.first)
            },
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Text(
            text = option.second,
        )
    }
}

@Preview
@Composable
private fun SortByDialogPreview() {
    JellyCastTheme {
        SortByDialog(
            currentSortBy = SortBy.NAME,
            currentSortOrder = SortOrder.ASCENDING,
            onUpdate = { _, _ -> },
            onDismissRequest = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SortByDialogItemPreview() {
    JellyCastTheme {
        SortByDialogItem(
            option = Pair(SortBy.NAME, "Title"),
            isSelected = true,
            onSelect = {},
        )
    }
}
