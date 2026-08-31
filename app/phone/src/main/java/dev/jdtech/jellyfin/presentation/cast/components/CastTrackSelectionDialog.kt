package dev.jdtech.jellyfin.presentation.cast.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import dev.jdtech.jellyfin.player.core.R
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CastTrackSelectionDialog(
    visible: Boolean,
    type: @C.TrackType Int,
    tracks: List<Track>,
    onSetTrack: (Track?) -> Unit,
    onDismiss: () -> Unit,
    displayExtraInfo: Boolean,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetY, label = "offset")

    LaunchedEffect(visible) {
        if (visible) {
            offsetY = 0f
        }
    }

    val listState = rememberLazyListState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f && !listState.canScrollBackward) {
                    offsetY += delta
                    return Offset(0f, delta)
                }
                if (delta < 0f) {
                    if (offsetY > 0f) {
                        val consumed = delta.coerceAtLeast(-offsetY)
                        offsetY += consumed
                        return Offset(0f, consumed)
                    }
                    if (!listState.canScrollForward) {
                        return Offset(0f, delta)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY > 150f) {
                    onDismiss()
                }
                offsetY = 0f
                return Velocity.Zero
            }
        }
    }

    val titleResource =
        when (type) {
            C.TRACK_TYPE_AUDIO -> R.string.select_audio_track
            C.TRACK_TYPE_TEXT -> R.string.select_subtitle_track
            else -> throw IllegalStateException("TrackType must be AUDIO or TEXT")
        }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = true)
                        var isDragging = false
                        var dragY = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.isConsumed) break

                            if (change.changedToUp()) {
                                if (!isDragging) {
                                    onDismiss()
                                } else if (offsetY > 150f) {
                                    onDismiss()
                                }
                                offsetY = 0f
                                break
                            }

                            val deltaY = change.position.y - change.previousPosition.y
                            val deltaX = change.position.x - change.previousPosition.x
                            dragY += deltaY
                            if (!isDragging && (abs(dragY) > viewConfiguration.touchSlop || abs(deltaX) > viewConfiguration.touchSlop)) {
                                isDragging = true
                            }
                            
                            if (isDragging) {
                                offsetY = dragY.coerceAtLeast(0f)
                            }
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val containerHeightDp = with(density) { windowInfo.containerSize.height.toDp() }

            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val screenMaxHeight = if (isLandscape) {
                containerHeightDp
            } else {
                containerHeightDp * 0.6f
            }

            Surface(
                modifier = modifier
                    .padding(MaterialTheme.spacings.small)
                    .widthIn(max = 500.dp)
                    .then(
                        if (isLandscape) Modifier.fillMaxHeight()
                        else Modifier.heightIn(max = screenMaxHeight)
                    )
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .offset { IntOffset(0, animatedOffset.roundToInt()) }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                
                                if (change.changedToUp()) {
                                    if (offsetY > 150f) {
                                        onDismiss()
                                    }
                                    offsetY = 0f
                                    break
                                }

                                // Only handle if not already consumed by children (like the list)
                                if (!change.isConsumed) {
                                    val deltaY = change.position.y - change.previousPosition.y
                                    val deltaX = change.position.x - change.previousPosition.x
                                    
                                    if (abs(deltaY) > 0) {
                                        offsetY = (offsetY + deltaY).coerceAtLeast(0f)
                                        change.consume()
                                    } else if (abs(deltaX) > 0) {
                                        // Other swipes (horizontal): consume and do nothing
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Catch clicks to prevent closing when interacting with menu
                    ),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .nestedScroll(nestedScrollConnection)
                        .padding(MaterialTheme.spacings.medium)
                        .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))

                    Text(
                        text = stringResource(titleResource),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(8.dp)
                    )

                    Box(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val scrollbarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                    drawContent()
                                    val edgeHeight = 16.dp.toPx()

                                    if (listState.canScrollBackward) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startY = 0f,
                                                endY = edgeHeight
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }

                                    if (listState.canScrollForward) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Black, Color.Transparent),
                                                startY = size.height - edgeHeight,
                                                endY = size.height
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }

                                    // Scrollbar drawing
                                    val layoutInfo = listState.layoutInfo
                                    val visibleItems = layoutInfo.visibleItemsInfo
                                    if (visibleItems.isNotEmpty() && layoutInfo.totalItemsCount > visibleItems.size) {
                                        val totalItems = layoutInfo.totalItemsCount
                                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                        
                                        if (viewportHeight > 0) {
                                            // Use average item size for a much more stable thumb height
                                            val avgItemSize = visibleItems.map { it.size }.average().toFloat()
                                            val estimatedTotalHeight = avgItemSize * totalItems
                                            
                                            val thumbHeight = (viewportHeight.toFloat() / estimatedTotalHeight * viewportHeight)
                                                .coerceIn(32.dp.toPx(), viewportHeight.toFloat())
                                            
                                            // Calculate exact scroll offset in pixels
                                            val firstItem = visibleItems.first()
                                            val scrollOffset = firstItem.index * avgItemSize - firstItem.offset
                                            val maxScrollOffset = estimatedTotalHeight - viewportHeight
                                            
                                            if (maxScrollOffset > 0) {
                                                val thumbOffset = (scrollOffset / maxScrollOffset) * (viewportHeight - thumbHeight)

                                                drawRoundRect(
                                                    color = scrollbarColor,
                                                    topLeft = Offset(size.width - 4.dp.toPx(), thumbOffset.coerceIn(0f, viewportHeight - thumbHeight)),
                                                    size = Size(3.dp.toPx(), thumbHeight),
                                                    cornerRadius = CornerRadius(2.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                        ) {
                            if (type == C.TRACK_TYPE_TEXT) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = { onSetTrack(null) })
                                            .padding(vertical = 4.dp)
                                            .padding(start = 8.dp, end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = tracks.none { it.selected },
                                            onClick = { onSetTrack(null) })
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.none),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            items(tracks, key = { it.id }) { track ->
                                TrackRow(
                                    track = track,
                                    displayExtraInfo = displayExtraInfo,
                                    onClick = { onSetTrack(track) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    displayExtraInfo: Boolean,
    onClick: (Track) -> Unit
) {
    val configuration = LocalConfiguration.current
    val currentLocale = configuration.locales[0]

    val trackIndexName = stringResource(R.string.track_index, track.id)

    val displayName = remember(track, currentLocale) {
        val locale = track.language?.takeIf { it.isNotBlank() && it != "und" }?.let { lang ->
            Locale.forLanguageTag(lang.replace("_", "-"))
        }

        val localizedLanguage = locale?.getDisplayLanguage(currentLocale)?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
        }

        localizedLanguage ?: track.label ?: trackIndexName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(track) })
            .padding(vertical = 4.dp)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = track.selected, onClick = { onClick(track) })

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(16.dp))

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val edgeWidth = 16.dp.toPx()

                    if (scrollState.value > 0) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startX = 0f,
                                endX = edgeWidth
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }

                    if (scrollState.value < scrollState.maxValue) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = size.width - edgeWidth,
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (track.isForced == true) {
                TrackMetadataBarItem(stringResource(R.string.forced))
            }
            if (track.isHearingImpaired == true) {
                TrackMetadataBarItem(stringResource(R.string.hearing_impaired))
            }
            if (displayExtraInfo) {
                if (track.isExternal == true) {
                    TrackMetadataBarItem(stringResource(R.string.external))
                }
                track.codec?.takeIf { it.isNotBlank() }?.let {
                    TrackMetadataBarItem(it)
                }
            }
        }
    }
}

@Composable
fun TrackMetadataBarItem(trackSpec: String, @DrawableRes icon: Int? = null) {
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(
                    horizontal = MaterialTheme.spacings.small,
                    vertical = MaterialTheme.spacings.extraSmall,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
        Text(text = trackSpec, style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(heightDp = 250)
@Composable
private fun CastTrackSelectionDialogPreview() {
    FindroidTheme {
        CastTrackSelectionDialog(
            visible = true,
            type = C.TRACK_TYPE_AUDIO,
            tracks = listOf(
                Track(3, "English", "eng", "aac", selected = false, supported = true),
                Track(4, "Spanish", "spa", "aac", selected = true, supported = true)
            ),
            onSetTrack = {},
            onDismiss = {},
            displayExtraInfo = false
        )
    }
}

@Preview(heightDp = 400)
@Composable
private fun CastTrackSubsSelectionSheetPreview() {
    FindroidTheme {
        CastTrackSelectionDialog(
            visible = true,
            type = C.TRACK_TYPE_TEXT,
            tracks = listOf(
                Track(
                    3,
                    "English",
                    "eng",
                    "subrip",
                    selected = false,
                    supported = true,
                    isForced = true
                ),
                Track(
                    4,
                    "Spanish",
                    "spa",
                    "webvvt",
                    selected = true,
                    supported = true,
                    isHearingImpaired = true,
                    isExternal = true
                ),
                Track(
                    5,
                    null,
                    null,
                    "webvvt",
                    selected = false,
                    supported = true,
                    isHearingImpaired = false,
                    isExternal = false
                )
            ),
            onSetTrack = {},
            onDismiss = {},
            displayExtraInfo = true
        )
    }
}