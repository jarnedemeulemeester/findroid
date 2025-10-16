package dev.jdtech.jellyfin.film.presentation.collection

import dev.jdtech.jellyfin.models.CollectionSection

data class CollectionState(
    val sections: List<CollectionSection> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
