package dev.jdtech.jellyfin.models

import java.util.UUID

data class View(
    val id: UUID,
    val name: String,
    val items: List<FindroidItem>,
    val type: CollectionType,
)
