package dev.jdtech.jellyfin.models

import java.util.UUID

data class View(
    val id: UUID,
    val name: String?,
    var items: List<JellyfinItem>? = null,
    val type: String?
)
