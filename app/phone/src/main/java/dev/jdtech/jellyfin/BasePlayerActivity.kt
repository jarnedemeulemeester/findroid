package dev.jdtech.jellyfin

import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.media3.session.MediaSession
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel

abstract class BasePlayerActivity : AppCompatActivity() {

    abstract val viewModel: PlayerActivityViewModel

    private lateinit var mediaSession: MediaSession

    override fun onStart() {
        super.onStart()

        mediaSession = MediaSession.Builder(this, viewModel.player).build()
    }

    override fun onResume() {
        super.onResume()

        viewModel.player.playWhenReady = viewModel.playWhenReady
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()

        viewModel.playWhenReady = viewModel.player.playWhenReady == true
        viewModel.player.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()

        mediaSession.release()
    }

    @Suppress("DEPRECATION")
    protected fun hideSystemUI() {
        // These methods are deprecated but we still use them because the new WindowInsetsControllerCompat has a bug which makes the action bar reappear
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    protected fun isRendererType(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        rendererIndex: Int,
        type: Int,
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (trackGroupArray.length == 0) {
            return false
        }
        val trackType = mappedTrackInfo.getRendererType(rendererIndex)
        return type == trackType
    }

    protected fun configureInsets(playerControls: View) {
        playerControls
            .setOnApplyWindowInsetsListener { _, windowInsets ->
                val cutout = windowInsets.displayCutout
                playerControls.updatePadding(
                    left = cutout?.safeInsetLeft ?: 0,
                    top = cutout?.safeInsetTop ?: 0,
                    right = cutout?.safeInsetRight ?: 0,
                    bottom = cutout?.safeInsetBottom ?: 0,
                )
                return@setOnApplyWindowInsetsListener windowInsets
            }
    }
}
