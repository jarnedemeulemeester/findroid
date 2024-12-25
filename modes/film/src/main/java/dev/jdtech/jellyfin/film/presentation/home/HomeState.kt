package dev.jdtech.jellyfin.film.presentation.home

import dev.jdtech.jellyfin.models.HomeItem

data class HomeState(
    val isOffline: Boolean = false,
    val sections: List<HomeItem.Section> = emptyList(),
    val views: List<HomeItem.ViewItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
