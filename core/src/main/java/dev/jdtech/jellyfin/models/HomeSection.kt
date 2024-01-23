package dev.jdtech.jellyfin.models

import java.util.UUID

data class HomeSection(
    val id: UUID,
    val name: UiText,
    var items: List<FindroidItem>,
)
