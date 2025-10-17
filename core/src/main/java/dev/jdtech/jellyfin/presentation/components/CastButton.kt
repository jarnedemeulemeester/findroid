package dev.jdtech.jellyfin.presentation.components

import android.content.Context
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import timber.log.Timber

private fun isChromecastEnabled(context: Context): Boolean {
    val prefsName = context.packageName + "_preferences"
    val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("pref_chromecast_enabled", true)
}

/**
 * Composable Cast Button that shows the native Chromecast selector
 * Only shows if Chromecast is enabled in settings
 */
@Composable
fun CastButton() {
    val context = LocalContext.current
    
    // Only show if Chromecast is enabled in settings
    if (!isChromecastEnabled(context)) {
        return
    }
    
    AndroidView(
        factory = { ctx ->
            MediaRouteButton(ctx).apply {
                try {
                    CastButtonFactory.setUpMediaRouteButton(ctx, this)
                } catch (e: Exception) {
                    Timber.e(e, "Error setting up Cast button")
                }
            }
        }
    )
}

/**
 * Setup Cast button in an AppCompatActivity menu
 * Only sets up if Chromecast is enabled in settings
 */
fun setupCastButton(activity: AppCompatActivity, menu: Menu, menuItemId: Int) {
    // Only set up if Chromecast is enabled in settings
    if (!isChromecastEnabled(activity)) {
        // Hide the menu item if Chromecast is disabled
        menu.findItem(menuItemId)?.isVisible = false
        return
    }
    
    try {
        val castContext = CastContext.getSharedInstance(activity)
        val mediaRouteMenuItem = menu.findItem(menuItemId)
        CastButtonFactory.setUpMediaRouteButton(
            activity.applicationContext,
            menu,
            menuItemId
        )
    } catch (e: Exception) {
        Timber.e(e, "Error setting up Cast button in menu")
    }
}
