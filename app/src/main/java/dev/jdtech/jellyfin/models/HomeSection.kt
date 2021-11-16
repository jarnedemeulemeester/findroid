package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto

data class HomeSection(
    val name: String,
    var items: List<BaseItemDto>
)