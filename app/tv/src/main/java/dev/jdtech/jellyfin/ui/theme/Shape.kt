package dev.jdtech.jellyfin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Shapes as ShapesTv

val shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
)

@OptIn(ExperimentalTvMaterial3Api::class)
val shapesTv = ShapesTv(
    extraSmall = shapes.extraSmall,
    small = shapes.small,
)
