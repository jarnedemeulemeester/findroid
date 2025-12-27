package dev.jdtech.jellyfin.setup.presentation.login

sealed interface LoginAction {
    data class OnLoginClick(val username: String, val password: String) : LoginAction

    data object OnChangeServerClick : LoginAction

    data object OnQuickConnectClick : LoginAction

    data object OnBackClick : LoginAction
}
