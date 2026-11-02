package dev.jdtech.jellyfin.setup.presentation.addserver

import android.content.Intent

sealed interface AddServerEvent {
    data object Success : AddServerEvent

    data class LaunchIntent(val intent: Intent) : AddServerEvent
}
