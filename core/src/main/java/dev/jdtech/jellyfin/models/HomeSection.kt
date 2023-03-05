package dev.jdtech.jellyfin.models

import java.util.UUID

data class HomeSection(
    val id: UUID,
    val name: String,
    var items: List<FindroidItem>
)
