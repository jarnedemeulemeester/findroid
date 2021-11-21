package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class View(
    val id: UUID,
    val name: String?,
    var items: List<BaseItemDto>? = null,
    val type: String?
)