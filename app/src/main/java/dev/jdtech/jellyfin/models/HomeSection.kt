package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class HomeSection(
    val name: String,
    var items: List<BaseItemDto>,
    val id: UUID = UUID.randomUUID()
)