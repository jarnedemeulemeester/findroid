package dev.jdtech.jellyfin.models

import java.util.*

data class DownloadSection(
    val id: UUID,
    val name: String,
    val items: List<PlayerItem>? = null,
    val series: List<DownloadSeriesMetadata>? = null
)