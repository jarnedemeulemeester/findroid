package dev.jdtech.jellyfin.setup.presentation.login

sealed interface LoginEvent {
    data object Success : LoginEvent
}
