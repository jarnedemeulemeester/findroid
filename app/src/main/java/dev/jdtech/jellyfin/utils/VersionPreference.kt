package dev.jdtech.jellyfin.utils

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

class VersionPreference(context: Context, attrs: AttributeSet): Preference(context, attrs) {
    init {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        summary = versionName
    }
}