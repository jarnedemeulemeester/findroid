package dev.jdtech.jellyfin.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import dev.jdtech.jellyfin.settings.domain.models.Permission

/** Returns whether the given runtime permission is currently granted. */
fun isPermissionGranted(context: Context, permission: Permission): Boolean =
    context.checkSelfPermission(permission.id) == PackageManager.PERMISSION_GRANTED

/** Returns whether the system would show a rationale dialog; only meaningful after a denial. */
fun shouldShowPermissionRationale(activity: Activity, permission: Permission): Boolean =
    activity.shouldShowRequestPermissionRationale(permission.id)

/** Opens the system settings page of this app, e.g. to grant a permanently denied permission. */
fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    context.startActivity(intent)
}
