package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.core.net.toUri
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.PlayerMediaType
import dev.jdtech.jellyfin.player.core.domain.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import com.google.android.gms.cast.CastDevice as GmsCastDevice

@Singleton
class CastManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CastManager {

    override val isSupported = true

    private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    override val availableDevices: StateFlow<List<CastDevice>> = _availableDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<CastDevice?>(null)
    override val connectedDevice: StateFlow<CastDevice?> = _connectedDevice.asStateFlow()

    private val _playbackState = MutableStateFlow(CastPlaybackState())
    override val playbackState: StateFlow<CastPlaybackState> = _playbackState.asStateFlow()

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    override val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
    override val audioTracks: StateFlow<List<Track>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
    override val subtitleTracks: StateFlow<List<Track>> = _subtitleTracks.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }
    private var castSession: CastSession? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val mediaRouter by lazy { MediaRouter.getInstance(context) }
    private val routeSelector by lazy {
        MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
            .build()
    }

    private val routeCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) { updateRoutes() }
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) { updateRoutes() }
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) { updateRoutes() }
    }

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() { updatePlaybackState() }
        override fun onMetadataUpdated() { updatePlaybackState() }
    }

    private fun updateRoutes() {
        val routes = mediaRouter.routes.filter { route ->
            val isCast = route.matchesSelector(routeSelector)
            val device = GmsCastDevice.getFromBundle(route.extras)
            isCast && device?.hasCapability(GmsCastDevice.CAPABILITY_VIDEO_OUT) == true
        }

        _availableDevices.value = routes.map { route ->
            val device = GmsCastDevice.getFromBundle(route.extras)
            CastDevice(device?.deviceId ?: route.id, route.name)
        }

        if (castSession == null) {
            _connectionState.value = if (routes.isNotEmpty()) CastConnectionState.AVAILABLE else CastConnectionState.DISCONNECTED
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionEnded(session: CastSession, error: Int) {
            _connectionState.value = CastConnectionState.DISCONNECTED
            clearSession()
            updateRoutes()
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _connectionState.value = CastConnectionState.CONNECTED
            setupSession(session)
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _connectionState.value = CastConnectionState.DISCONNECTED
            clearSession()
            updateRoutes()
        }
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _connectionState.value = CastConnectionState.CONNECTED
            setupSession(session)
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _connectionState.value = CastConnectionState.DISCONNECTED
            clearSession()
            updateRoutes()
        }
        override fun onSessionStarting(session: CastSession) {
            _connectionState.value = CastConnectionState.CONNECTING
            session.castDevice?.let {
                _connectedDevice.value = CastDevice(it.deviceId, it.friendlyName)
            }
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {
            _connectionState.value = CastConnectionState.CONNECTING
            session.castDevice?.let {
                _connectedDevice.value = CastDevice(it.deviceId, it.friendlyName)
            }
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    override fun init() {
        castContext.sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        mediaRouter.addCallback(routeSelector, routeCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        updateRoutes()
        
        castContext.sessionManager.currentCastSession?.let {
            _connectionState.value = CastConnectionState.CONNECTED
            setupSession(it)
        }
    }

    override fun connect(device: CastDevice) {
        val route = mediaRouter.routes.find {
            val gmsDevice = GmsCastDevice.getFromBundle(it.extras)
            (gmsDevice?.deviceId ?: it.id) == device.id
        }
        if (route != null) {
            _connectedDevice.value = device
            _connectionState.value = CastConnectionState.CONNECTING
            mediaRouter.selectRoute(route)
        }
    }

    override fun disconnect() {
        castContext.sessionManager.endCurrentSession(true)
    }

    private fun setupSession(session: CastSession) {
        castSession = session
        remoteMediaClient = session.remoteMediaClient
        session.castDevice?.let {
            _connectedDevice.value = CastDevice(it.deviceId, it.friendlyName)
        }
        remoteMediaClient?.registerCallback(remoteMediaClientCallback)
        startProgressUpdate()
    }

    private fun clearSession() {
        progressJob?.cancel()
        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        castSession = null
        remoteMediaClient = null
        _currentItem.value = null
        _connectedDevice.value = null
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(1000.milliseconds)
            }
        }
    }

    private fun updatePlaybackState() {
        val client = remoteMediaClient ?: return
        val isPlaying = client.isPlaying
        val position = client.approximateStreamPosition
        val duration = client.streamDuration
        _playbackState.value = CastPlaybackState(
            isPlaying = isPlaying,
            currentPosition = position,
            duration = duration
        )
        _volume.value = castSession?.volume?.toFloat() ?: 1f
        updateTracks(client)
    }

    private fun updateTracks(client: RemoteMediaClient) {
        val mediaInfo = client.mediaInfo ?: return
        val mediaTracks = mediaInfo.mediaTracks ?: emptyList()
        val activeTrackIds = client.mediaStatus?.activeTrackIds?.toList() ?: emptyList()

        val audio = mutableListOf<Track>()
        val subs = mutableListOf<Track>()

        mediaTracks.forEach { castTrack ->
            val track = Track(
                id = castTrack.id.toInt(),
                label = castTrack.name ?: "",
                language = castTrack.language ?: "",
                codec = castTrack.subtype.toString(),
                selected = activeTrackIds.contains(castTrack.id),
                supported = true
            )
            if (castTrack.type == com.google.android.gms.cast.MediaTrack.TYPE_AUDIO) {
                audio.add(track)
            } else if (castTrack.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT) {
                subs.add(track)
            }
        }
        _audioTracks.value = audio
        _subtitleTracks.value = subs
    }

    override fun loadItem(item: PlayerItem, startPosition: Long) {
        _currentItem.value = item
        val client = remoteMediaClient ?: return
        val mediaType = if (item.mediaType == PlayerMediaType.EPISODE) MediaMetadata.MEDIA_TYPE_TV_SHOW else MediaMetadata.MEDIA_TYPE_MOVIE
        val mediaMetadata = MediaMetadata(mediaType).apply {
            putString(MediaMetadata.KEY_TITLE, item.name)
            item.posterUrl?.let {
                addImage(WebImage(it.toUri()))
            }
        }

        val mediaInfoBuilder = MediaInfo.Builder(item.mediaSourceUri)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4")
            .setMetadata(mediaMetadata)

        if (item.externalSubtitles.isNotEmpty()) {
            val castTracks = item.externalSubtitles.mapIndexed { index, subtitle ->
                com.google.android.gms.cast.MediaTrack.Builder((index + 1).toLong(), com.google.android.gms.cast.MediaTrack.TYPE_TEXT)
                    .setName(subtitle.title)
                    .setSubtype(com.google.android.gms.cast.MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentId(subtitle.uri.toString())
                    .setContentType(subtitle.mimeType)
                    .setLanguage(subtitle.language)
                    .build()
            }
            mediaInfoBuilder.setMediaTracks(castTracks)
        }

        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfoBuilder.build())
            .setAutoplay(true)
            .setCurrentTime(startPosition)
            .build()

        client.load(loadRequest)
    }

    override fun play() {
        remoteMediaClient?.play()
    }

    override fun pause() {
        remoteMediaClient?.pause()
    }

    override fun seekTo(position: Long) {
        val options = MediaSeekOptions.Builder()
            .setPosition(position)
            .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
            .build()

        remoteMediaClient?.seek(options)
    }

    override fun setVolume(volume: Float) {
        castSession?.volume = volume.toDouble()
    }

    override fun setAudioTrack(track: Track?) {
        val client = remoteMediaClient ?: return
        val activeIds = client.mediaStatus?.activeTrackIds?.toMutableList() ?: mutableListOf()
        val audioTrackIds = _audioTracks.value.map { it.id.toLong() }
        activeIds.removeAll(audioTrackIds)
        if (track != null) {
            activeIds.add(track.id.toLong())
        }
        client.setActiveMediaTracks(activeIds.toLongArray())
    }

    override fun setSubtitleTrack(track: Track?) {
        val client = remoteMediaClient ?: return
        val activeIds = client.mediaStatus?.activeTrackIds?.toMutableList() ?: mutableListOf()
        val subTrackIds = _subtitleTracks.value.map { it.id.toLong() }
        activeIds.removeAll(subTrackIds)
        if (track != null) {
            activeIds.add(track.id.toLong())
        }
        client.setActiveMediaTracks(activeIds.toLongArray())
    }

    override fun stop() {
        remoteMediaClient?.stop()
    }
}

