package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.gms.cast.CastDevice as GmsCastDevice

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

    private val castStateListener = CastStateListener { state ->
        when (state) {
            CastState.CONNECTED -> _connectionState.value = CastConnectionState.CONNECTED

            CastState.CONNECTING -> _connectionState.value = CastConnectionState.CONNECTING

            else -> {
                _connectionState.value = CastConnectionState.DISCONNECTED
                _connectedDevice.value = null
                updateRoutes()
            }
        }
    }

    private fun updateRoutes() {
        val routes = mediaRouter.routes.filter { route ->
            val isCast = route.matchesSelector(routeSelector)
            val device = GmsCastDevice.getFromBundle(route.extras)
            (isCast && device?.hasCapability(GmsCastDevice.CAPABILITY_VIDEO_OUT) == true) || route.supportsControlCategory(
                MediaControlIntent.CATEGORY_REMOTE_PLAYBACK
            )
        }

        _availableDevices.value = routes.map { route ->
            ChromeCastDevice(route)
        }
    }

    override fun init() {
        castContext.addCastStateListener(castStateListener)
        mediaRouter.addCallback(
            routeSelector,
            routeCallback,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
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
}
