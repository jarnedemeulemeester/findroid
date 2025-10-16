package dev.jdtech.jellyfin.presentation.components

import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import timber.log.Timber

/**
 * Composable Cast Button that shows the Chromecast icon
 */
@Composable
fun CastButton() {
    val context = LocalContext.current
    
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
 */
fun setupCastButton(activity: AppCompatActivity, menu: Menu, menuItemId: Int) {
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
