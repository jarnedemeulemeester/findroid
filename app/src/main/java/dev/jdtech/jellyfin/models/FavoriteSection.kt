package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto

data class FavoriteSection(
    val id: Int,
    val name: String,
    var items: List<BaseItemDto>
)
