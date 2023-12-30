package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.ui.dummy.dummyEpisode
import dev.jdtech.jellyfin.ui.dummy.dummyMovie
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ItemCard(
    item: FindroidItem,
    direction: Direction,
    onClick: (FindroidItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = when (direction) {
        Direction.HORIZONTAL -> 260
        Direction.VERTICAL -> 150
    }
    Column(
        modifier = modifier
            .width(width.dp),
    ) {
        Surface(
            onClick = { onClick(item) },
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    BorderStroke(
                        4.dp,
                        Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ),
            ),
            scale = ClickableSurfaceScale.None,
        ) {
            Box {
                ItemPoster(
                    item = item,
                    direction = direction,
                )
                ProgressBadge(
                    item = item,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.spacings.small),
                )
                if (direction == Direction.HORIZONTAL) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(MaterialTheme.spacings.small),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(
                                    item.playbackPositionTicks
                                        .div(
                                            item.runtimeTicks.toFloat(),
                                        )
                                        .times(
                                            width - 16,
                                        ).dp,
                                )
                                .clip(
                                    MaterialTheme.shapes.extraSmall,
                                )
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
        Text(
            text = if (item is FindroidEpisode) item.seriesName else item.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = if (direction == Direction.HORIZONTAL) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item is FindroidEpisode) {
            Text(
                text = stringResource(
                    id = R.string.episode_name_extended,
                    item.parentIndexNumber,
                    item.indexNumber,
                    item.name,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun ItemCardPreviewMovie() {
    FindroidTheme {
        Surface {
            ItemCard(
                item = dummyMovie,
                direction = Direction.HORIZONTAL,
                onClick = {},
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun ItemCardPreviewMovieVertical() {
    FindroidTheme {
        Surface {
            ItemCard(
                item = dummyMovie,
                direction = Direction.VERTICAL,
                onClick = {},
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun ItemCardPreviewEpisode() {
    FindroidTheme {
        Surface {
            ItemCard(
                item = dummyEpisode,
                direction = Direction.HORIZONTAL,
                onClick = {},
            )
        }
    }
}
