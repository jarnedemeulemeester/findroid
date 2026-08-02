package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.SessionManagerListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class ChromeCastDevice(val route: MediaRouter.RouteInfo) : Device(
    id = route.id,
    name = route.name,
    enabled = route.isEnabled,
    supportsH265 = CastDevice.getFromBundle(route.extras)?.let {
        it.modelName.contains("Ultra", ignoreCase = true) ||
                it.modelName.contains("Google TV", ignoreCase = true)
    } ?: false
)

@Singleton
class CastSessionManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi,
) : CastSessionManager {

    override val isSupported: Boolean
        get() = jellyfinApi.api.baseUrl?.startsWith("https://", ignoreCase = true) ?: false

    private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(emptyList())
    override val availableDevices: StateFlow<List<Device>> = _availableDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Device?>(null)
    override val connectedDevice: StateFlow<Device?> = _connectedDevice.asStateFlow()

    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }
    private val mediaRouter by lazy { MediaRouter.getInstance(context) }
    private val routeSelector by lazy {
        MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_VIDEO_PLAYBACK)
            .build()
    }

    private val routeCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes()
        }

        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes()
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes()
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            Timber.d("Starting session")
            _connectedDevice.value = ChromeCastDevice(mediaRouter.selectedRoute)
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Timber.d("Session started")
            _connectedDevice.value = ChromeCastDevice(mediaRouter.selectedRoute)
            updateDiscovery(0)
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            Timber.d("Resuming session")
            _connectedDevice.value = ChromeCastDevice(mediaRouter.selectedRoute)
            updateDiscovery(MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            Timber.d("Session resumed")
            _connectedDevice.value = ChromeCastDevice(mediaRouter.selectedRoute)
            updateDiscovery(0)
        }

        override fun onSessionEnding(session: CastSession) {
            Timber.d("Session ending")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            Timber.d("Session ended")
            _connectedDevice.value = null
            updateDiscovery()
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            val reasonMessage = CastStatusCodes.getStatusCodeString(reason)
            Timber.d("Session suspended. Reason: $reasonMessage")
            updateDiscovery()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            val errorMessage = CastStatusCodes.getStatusCodeString(error)
            Timber.e("Session start failed: $errorMessage ($error)")
            _connectedDevice.value = null
            updateDiscovery()
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            val errorMessage = CastStatusCodes.getStatusCodeString(error)
            Timber.e("Session resume failed: $errorMessage ($error)")
            _connectedDevice.value = null
            updateDiscovery()
        }
    }

    private val castStateListener = CastStateListener { state ->
        _connectionState.value = when (state) {
            CastState.CONNECTED -> CastConnectionState.CONNECTED
            CastState.CONNECTING -> CastConnectionState.CONNECTING
            else -> CastConnectionState.DISCONNECTED
        }
    }

    override fun init() {
        castContext.addCastStateListener(castStateListener)
        castContext.sessionManager.addSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )

        updateDiscovery()
    }

    private fun updateRoutes() {
        val routes = mediaRouter.routes.filter { route ->
            // 1. Must match the selector (Video Playback) and not be the default route
            if (route.isDefault || !route.matchesSelector(routeSelector)) return@filter false

            // 2. Filter by device type (excludes single speakers)
            if (route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER) return@filter false

            // 3. Specific check on Cast capabilities (excludes speaker groups and audio-only devices)
            val castDevice = CastDevice.getFromBundle(route.extras)
            castDevice?.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT) ?: true
        }

        _availableDevices.value = routes.map { route ->
            ChromeCastDevice(route)
        }
    }

    override fun updateDiscovery(flags: Int) {
        mediaRouter.addCallback(
            routeSelector,
            routeCallback,
            flags
        )
    }

    override fun connect(device: Device) {
        if (castContext.castState == CastState.CONNECTED) disconnect()

        if (device is ChromeCastDevice) {
            mediaRouter.selectRoute(device.route)
        }
    }

    override fun disconnect() {
        castContext.sessionManager.endCurrentSession(true)
    }

    override fun release() {
        mediaRouter.removeCallback(routeCallback)

        castContext.removeCastStateListener(castStateListener)
        castContext.sessionManager.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }
}
