package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.SessionManagerListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ChromeCastDevice(val route: MediaRouter.RouteInfo) : Device(
    id = route.id,
    name = route.name,
    description = route.description,
    enabled = route.isEnabled
)

@Singleton
class CastSessionManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CastSessionManager {

    override val isSupported = true

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
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
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
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) = updateDiscovery(0)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) =
            updateDiscovery(0)

        override fun onSessionEnded(session: CastSession, error: Int) = updateDiscovery()
        override fun onSessionSuspended(session: CastSession, reason: Int) = updateDiscovery()
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }


    private val castStateListener = CastStateListener { state ->
        _connectionState.value = when (state) {
            CastState.CONNECTED -> CastConnectionState.CONNECTED
            CastState.CONNECTING -> CastConnectionState.CONNECTING
            else -> {
                _connectedDevice.value = null
                CastConnectionState.DISCONNECTED
            }
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
            !route.isDefault && route.matchesSelector(routeSelector)
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
        updateRoutes()
    }

    override fun connect(device: Device) {
        if (_connectionState.value != CastConnectionState.DISCONNECTED) disconnect()

        if (device is ChromeCastDevice) {
            _connectedDevice.value = device
            mediaRouter.selectRoute(device.route)
        }
    }

    override fun disconnect() {
        castContext.sessionManager.endCurrentSession(true)
        _connectedDevice.value = null
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
