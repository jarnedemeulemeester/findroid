package dev.jdtech.jellyfin.setup.presentation.permission

import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.domain.models.Permission
import dev.jdtech.jellyfin.settings.domain.models.Preference

/**
 * Describes a runtime permission the app wants to request from the user, together with everything
 * needed to display a rationale screen for it.
 *
 * Wraps the permission's [Permission] declaration from the settings module — the single source of
 * truth for its identity, applicable API level, name and icon — and adds only what the asking flow
 * itself needs: a rationale and the preference storing whether the user decided (granted or
 * skipped) this permission.
 */
data class PermissionRequest(
    val permission: Permission,
    @param:StringRes val rationale: Int,
    val decidedPreference: Preference<Boolean>,
)
