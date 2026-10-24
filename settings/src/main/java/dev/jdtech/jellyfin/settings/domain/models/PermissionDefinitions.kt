package dev.jdtech.jellyfin.settings.domain.models

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import dev.jdtech.jellyfin.settings.R

/**
 * The runtime permissions this app declares, each declared exactly once.
 */
object PermissionDefinitions {
    @SuppressLint("InlinedApi")
    val localNetwork =
        Permission(
            id = Manifest.permission.ACCESS_LOCAL_NETWORK,
            minSdk = Build.VERSION_CODES.CINNAMON_BUN,
            nameStringResource = R.string.permission_local_network,
            descriptionStringRes = R.string.permission_local_network_summary,
            iconDrawableId = R.drawable.ic_network,
        )

    /** All declared permissions, in the order they appear in the Permissions category. */
    val all: List<Permission> = listOf(localNetwork)
}
