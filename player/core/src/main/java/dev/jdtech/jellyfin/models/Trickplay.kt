package dev.jdtech.jellyfin.models

import android.graphics.Bitmap

data class Trickplay(
    val interval: Int,
    val images: List<Bitmap>,
)
