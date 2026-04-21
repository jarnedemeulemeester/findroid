package dev.jdtech.jellyfin.models

data class CollectionSection(val id: Int, val name: UiText, var items: List<FindroidItem>)
