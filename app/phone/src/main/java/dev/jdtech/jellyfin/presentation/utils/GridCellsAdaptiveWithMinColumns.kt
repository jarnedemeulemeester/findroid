package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Based on [GridCells.Adaptive] with the added [minColumns] variable.
 * [minColumns] takes precedence over [minSize].
 */
class GridCellsAdaptiveWithMinColumns(private val minSize: Dp, private val minColumns: Int) : GridCells {
    init {
        require(minSize > 0.dp) { "Provided min size $minSize should be larger than zero." }
        require(minColumns > 0) { "Provided min columns $minColumns should be larger than zero." }
    }

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), minColumns)
        return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
    }

    private fun calculateCellsCrossAxisSizeImpl(
        gridSize: Int,
        slotCount: Int,
        spacing: Int,
    ): List<Int> {
        val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
        val slotSize = gridSizeWithoutSpacing / slotCount
        val remainingPixels = gridSizeWithoutSpacing % slotCount
        return List(slotCount) {
            slotSize + if (it < remainingPixels) 1 else 0
        }
    }

    override fun hashCode(): Int {
        return minSize.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is GridCellsAdaptiveWithMinColumns && minSize == other.minSize && minColumns == other.minColumns
    }
}
