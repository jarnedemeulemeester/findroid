package dev.jdtech.jellyfin.models

data class FavoriteSection(
    val id: Int,
    val name: UiText,
    var items: List<FindroidItem>,
)
