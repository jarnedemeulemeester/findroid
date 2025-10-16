package dev.jdtech.jellyfin.film.presentation.collection

import dev.jdtech.jellyfin.models.CollectionSection

data class CollectionState(
    val sections: List<CollectionSection> = emptyList(),
    val allSections: List<CollectionSection> = emptyList(), // Original sections without filters
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
