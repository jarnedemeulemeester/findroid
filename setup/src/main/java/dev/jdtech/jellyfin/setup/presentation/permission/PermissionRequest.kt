package dev.jdtech.jellyfin.setup.presentation.permission

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.domain.models.Preference

/**
 * Describes a runtime permission the app wants to request from the user, together with everything
 * needed to display a rationale screen for it.
 *
 * @param permission The runtime permission string to declare, check and request.
 * @param applicableSince The API level from which the permission exists and is enforced. Below this
 *   level the permission is unknown to the OS, requesting it fails instantly and all permission UI
 *   must be skipped.
 * @param decidedPreference The preference storing whether the user decided (granted or skipped)
 *   this permission. The flag is only ever set on devices at or above [applicableSince].
 */
data class PermissionRequest(
    val permission: String,
    val applicableSince: Int,
    @param:StringRes val title: Int,
    @param:StringRes val rationale: Int,
    @param:DrawableRes val icon: Int,
    val decidedPreference: Preference<Boolean>,
)
