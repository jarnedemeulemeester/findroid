package dev.jdtech.jellyfin.setup.presentation.permission

import dev.jdtech.jellyfin.settings.domain.models.Permission
import dev.jdtech.jellyfin.settings.domain.models.Preference

/** A runtime permission the app asks the user to grant. */
data class PermissionRequest(
    val permission: Permission,
    val decidedPreference: Preference<Boolean>,
)
