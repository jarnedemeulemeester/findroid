package dev.jdtech.jellyfin.player.core.domain.models

import android.graphics.Bitmap

data class Trickplay(
    val interval: Int,
    val images: List<Bitmap>,
)
