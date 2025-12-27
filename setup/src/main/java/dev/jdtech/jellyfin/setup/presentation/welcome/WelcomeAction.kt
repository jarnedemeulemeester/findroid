package dev.jdtech.jellyfin.setup.presentation.welcome

sealed interface WelcomeAction {
    data object OnContinueClick : WelcomeAction

    data object OnLearnMoreClick : WelcomeAction
}
