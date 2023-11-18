package dev.jdtech.jellyfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.TabRow

/**
 * Adds a pill shaped border indicator behind the tab
 *
 * @param currentTabPosition position of the current selected tab
 * @param doesTabRowHaveFocus whether any tab in TabRow is focused
 * @param modifier modifier to be applied to the indicator
 * @param activeBorderColor color of border when [TabRow] is active
 * @param inactiveBorderColor color of border when [TabRow] is inactive
 *
 * This component is adapted from androidx.tv.material3.TabRowDefaults.PillIndicator
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PillBorderIndicator(
    currentTabPosition: DpRect,
    doesTabRowHaveFocus: Boolean,
    modifier: Modifier = Modifier,
    activeBorderColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveBorderColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
) {
    val width by animateDpAsState(
        targetValue = currentTabPosition.width,
        label = "PillIndicator.width",
    )
    val height = currentTabPosition.height
    val leftOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        label = "PillIndicator.leftOffset",
    )
    val topOffset = currentTabPosition.top

    val borderColor by
        animateColorAsState(
            targetValue = if (doesTabRowHaveFocus) activeBorderColor else inactiveBorderColor,
            label = "PillIndicator.pillColor",
        )

    Box(
        modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = leftOffset, y = topOffset)
            .width(width)
            .height(height)
            .border(width = 4.dp, color = borderColor, shape = RoundedCornerShape(50))
            .zIndex(-1f),
    )
}
