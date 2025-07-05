package dev.jdtech.jellyfin.film.presentation.collection

import dev.jdtech.jellyfin.models.FavoriteSection

data class CollectionState(
    val sections: List<FavoriteSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
