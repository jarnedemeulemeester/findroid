package dev.jdtech.jellyfin.film.presentation.home

import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidItem

sealed interface HomeAction {
    data class OnItemClick(val item: FindroidItem) : HomeAction
    data class OnLibraryClick(val library: FindroidCollection) : HomeAction
    data object OnRetryClick : HomeAction
    data object OnSettingsClick : HomeAction
}
