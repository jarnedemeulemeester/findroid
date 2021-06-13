package dev.jdtech.jellyfin.models

import java.util.*

data class ViewItem(
    val id: UUID,
    val name: String?,
    val primaryImageUrl: String
)