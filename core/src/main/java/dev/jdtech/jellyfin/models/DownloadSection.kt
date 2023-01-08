package dev.jdtech.jellyfin.models

import java.util.UUID

data class DownloadSection(
    val id: UUID,
    val name: String,
    val items: List<PlayerItem>? = null,
    val series: List<dev.jdtech.jellyfin.models.DownloadSeriesMetadata>? = null
)
