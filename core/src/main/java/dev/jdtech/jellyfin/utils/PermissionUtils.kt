package dev.jdtech.jellyfin.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/** Returns whether the given runtime permission is currently granted. */
fun isPermissionGranted(context: Context, permission: String): Boolean =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/**
 * Returns whether the system would show a permission rationale dialog for the given permission.
 * Only meaningful after the permission was denied at least once: before the first request this
 * returns `false` as well.
 */
fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean =
    activity.shouldShowRequestPermissionRationale(permission)

/** Opens the system settings page of this app, e.g. to grant a permanently denied permission. */
fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    context.startActivity(intent)
}
