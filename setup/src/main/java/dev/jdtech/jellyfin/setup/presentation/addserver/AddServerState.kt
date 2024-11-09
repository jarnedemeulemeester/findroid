package dev.jdtech.jellyfin.setup.presentation.addserver

import dev.jdtech.jellyfin.models.UiText

data class AddServerState(
    val isLoading: Boolean = false,
    val error: Collection<UiText>? = null,
)
