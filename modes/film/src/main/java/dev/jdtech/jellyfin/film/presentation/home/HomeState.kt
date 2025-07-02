package dev.jdtech.jellyfin.film.presentation.home

import dev.jdtech.jellyfin.models.HomeItem

data class HomeState(
    val isOffline: Boolean = false,
    val suggestionsSection: HomeItem.Suggestions? = null,
    val resumeSection: HomeItem.Section? = null,
    val nextUpSection: HomeItem.Section? = null,
    val views: List<HomeItem.ViewItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
