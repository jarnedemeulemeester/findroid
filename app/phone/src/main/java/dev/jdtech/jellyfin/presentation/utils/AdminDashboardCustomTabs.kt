package dev.jdtech.jellyfin.presentation.utils

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

fun Context.launchAdminDashboardCustomTab(serverAddress: String) {
    val uri = "${serverAddress.trimEnd('/')}/web/#/dashboard".toUri()
    CustomTabsIntent.Builder().build().launchUrl(this, uri)
}