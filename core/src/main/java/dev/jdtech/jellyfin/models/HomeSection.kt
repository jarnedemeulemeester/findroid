package dev.jdtech.jellyfin.models

import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class HomeSection(
    val id: UUID,
    val name: String,
    var items: List<BaseItemDto>
)
