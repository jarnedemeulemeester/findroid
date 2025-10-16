package dev.jdtech.jellyfin.presentation.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import dev.jdtech.jellyfin.fragments.DownloadsFragment

/**
 * Host for the Downloads Fragment inside Compose navigation.
 * Creates and manages the Fragment lifecycle properly.
 */
@Composable
fun DownloadsScreenHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fragmentManager = remember { (context as FragmentActivity).supportFragmentManager }
    val containerId = remember { android.view.View.generateViewId() }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            android.util.Log.d("DownloadsScreenHost", "Factory: Creating FragmentContainerView with id=$containerId")
            
            FragmentContainerView(ctx).apply {
                id = containerId
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // Add the Fragment to the container
                fragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(containerId, DownloadsFragment::class.java, null, "downloads_fragment")
                    android.util.Log.d("DownloadsScreenHost", "Fragment transaction committed")
                }
            }
        },
        update = { view ->
            android.util.Log.d("DownloadsScreenHost", "Update: Refreshing fragment view")
            view.visibility = android.view.View.VISIBLE
            view.requestLayout()
            view.invalidate()
        }
    )
    
    DisposableEffect(containerId) {
        android.util.Log.d("DownloadsScreenHost", "DisposableEffect: Fragment container created")
        onDispose {
            android.util.Log.d("DownloadsScreenHost", "DisposableEffect: Cleaning up fragment")
            // Remove the fragment when the composable is disposed
            fragmentManager.findFragmentByTag("downloads_fragment")?.let { fragment ->
                fragmentManager.commit {
                    remove(fragment)
                }
            }
        }
    }
}
