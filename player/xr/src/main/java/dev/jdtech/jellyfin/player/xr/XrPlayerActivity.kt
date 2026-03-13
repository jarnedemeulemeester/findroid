package dev.jdtech.jellyfin.player.xr

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.session.MediaSession
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.scene
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import timber.log.Timber

@AndroidEntryPoint
class XrPlayerActivity : AppCompatActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private var xrSession: Session? = null
    private var mediaSession: MediaSession? = null
    private var currentStereoMode: String = "mono"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure window is transparent so we can see the XR scene behind the UI
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))

        val itemIdString = intent.extras?.getString("itemId")
        if (itemIdString == null) {
            finish()
            return
        }
        val itemId = UUID.fromString(itemIdString)
        val itemKind = intent.extras!!.getString("itemKind") ?: ""
        val startFromBeginning = intent.extras!!.getBoolean("startFromBeginning")
        currentStereoMode = intent.extras?.getString("stereoMode") ?: "mono"

        // Initialize XR Session
        try {
            val result = Session.create(this)
            if (result is SessionCreateSuccess) {
                xrSession = result.session
                
                // Request full space mode for 3D support and immersive playback
                try {
                    val capabilities = xrSession?.scene?.spatialCapabilities
                    if (capabilities?.contains(androidx.xr.scenecore.SpatialCapability.SPATIAL_3D_CONTENT) == true) {
                        xrSession?.scene?.requestFullSpaceMode()
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to request full space mode")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "XR session not available")
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val session = xrSession
                if (session != null) {
                    SpatialPlayerScreen(
                        viewModel = viewModel,
                        session = session,
                        initialStereoMode = currentStereoMode,
                        itemId = itemId,
                        itemKind = itemKind,
                        startFromBeginning = startFromBeginning,
                        onBackClick = { finish() }
                    )
                } else {
                    // Fallback UI or close if session failed
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("XR Session not available", color = Color.White)
                        LaunchedEffect(Unit) {
                            delay(2000)
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaSession = MediaSession.Builder(this, viewModel.player).build()
    }

    override fun onResume() {
        super.onResume()
        viewModel.player.playWhenReady = viewModel.playWhenReady
    }

    override fun onPause() {
        super.onPause()
        viewModel.playWhenReady = viewModel.player.playWhenReady
        viewModel.player.playWhenReady = false
        viewModel.updatePlaybackProgress()
    }

    override fun onStop() {
        super.onStop()
        try {
            if (!isFinishing) {
                xrSession?.scene?.requestHomeSpaceMode()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to request home space mode")
        }
        mediaSession?.release()
        mediaSession = null
    }

    override fun onDestroy() {
        super.onDestroy()
        xrSession = null
    }
}
