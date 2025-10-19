package dev.jdtech.jellyfin.film.presentation.home

import dev.jdtech.jellyfin.models.JellyCastCollection
import dev.jdtech.jellyfin.models.JellyCastItem

sealed interface HomeAction {
    data class OnItemClick(val item: JellyCastItem) : HomeAction
    data class OnLibraryClick(val library: JellyCastCollection) : HomeAction
    data object OnRetryClick : HomeAction
    data object OnSettingsClick : HomeAction
    data object OnManageServers : HomeAction
}
