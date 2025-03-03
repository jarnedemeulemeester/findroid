package dev.jdtech.jellyfin.setup.presentation.users

import java.util.UUID

sealed interface UsersAction {
    data class OnUserClick(val userId: UUID) : UsersAction
    data class OnPublicUserClick(val username: String) : UsersAction
    data class OnDeleteUser(val userId: UUID) : UsersAction
    data object OnChangeServerClick : UsersAction
    data object OnAddClick : UsersAction
    data object OnBackClick : UsersAction
}
