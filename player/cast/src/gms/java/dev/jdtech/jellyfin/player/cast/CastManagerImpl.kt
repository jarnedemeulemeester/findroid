package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.core.net.toUri
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
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
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import com.google.android.gms.cast.CastDevice as GmsCastDevice

data class ChromeCastDevice(val route: MediaRouter.RouteInfo) : Device(
    id = route.id,
    name = route.name,
    description = route.description,
    enabled = route.isEnabled
)

/**
 * Real implementation of [CastManager] used in the `gms` build flavor.
 * Wraps the Google Cast framework ([CastContext], [RemoteMediaClient], [SessionManager])
 * and translates standard Findroid [PlayerItem]s into [MediaQueueItem]s that can be pushed
 * to Chromecast devices. Also syncs playback state natively.
 */
@Singleton
class CastManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi
) : CastManager {

    override val isSupported = true

    private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(emptyList())
    override val availableDevices: StateFlow<List<Device>> = _availableDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Device?>(null)
    override val connectedDevice: StateFlow<Device?> = _connectedDevice.asStateFlow()

    private val _playbackState = MutableStateFlow(CastPlaybackState())
    override val playbackState: StateFlow<CastPlaybackState> = _playbackState.asStateFlow()

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    override val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    private val _restoringItemId = MutableStateFlow<String?>(null)
    override val restoringItemId: StateFlow<String?> = _restoringItemId.asStateFlow()

    private val _activeTrackIds = MutableStateFlow<List<Long>>(emptyList())
    override val activeTrackIds: StateFlow<List<Long>> = _activeTrackIds.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
    override val subtitleTracks: StateFlow<List<Track>> = _subtitleTracks.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }
    private var castSession: CastSession? = null
    private var remoteMediaClient: RemoteMediaClient? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val itemCache = mutableMapOf<String, PlayerItem>()

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

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updatePlaybackState()
            remoteMediaClient?.let { updateTracks(it) }
        }

        override fun onMetadataUpdated() {
            updatePlaybackState()
            remoteMediaClient?.let { updateTracks(it) }
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

        if (castSession == null) {
            _connectionState.value =
                if (routes.isNotEmpty()) CastConnectionState.AVAILABLE else CastConnectionState.DISCONNECTED
        }
    }

    private val castStateListener = CastStateListener { state ->
        when (state) {
            CastState.CONNECTED -> {
                _connectionState.value = CastConnectionState.CONNECTED
                castContext.sessionManager.currentCastSession?.let { session ->
                    setupSession(session)
                }
            }

            CastState.NOT_CONNECTED -> {
                _connectionState.value = CastConnectionState.DISCONNECTED
                clearSession()
                updateRoutes()
            }

            CastState.CONNECTING -> {
                _connectionState.value = CastConnectionState.CONNECTING
                castContext.sessionManager.currentCastSession?.castDevice?.let { device ->
                    _connectedDevice.value = Device(device.deviceId, device.friendlyName)
                }
            }

            CastState.NO_DEVICES_AVAILABLE -> {
                _connectionState.value = CastConnectionState.DISCONNECTED
                clearSession()
            }
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

        castContext.sessionManager.currentCastSession?.let {
            setupSession(it)
        }
    }

    override fun connect(device: Device) {
        if (device is ChromeCastDevice) {
            _connectedDevice.value = device
            _connectionState.value = CastConnectionState.CONNECTING
            mediaRouter.selectRoute(device.route)
        }
    }

    override fun disconnect() {
        castContext.sessionManager.endCurrentSession(true)
    }

    private fun setupSession(session: CastSession) {
        castSession = session
        remoteMediaClient = session.remoteMediaClient
        session.castDevice?.let {
            _connectedDevice.value = Device(it.deviceId, it.friendlyName)
        }
        remoteMediaClient?.registerCallback(remoteMediaClientCallback)

        startProgressUpdate()

        _connectionState.value = CastConnectionState.CONNECTED
    }

    private fun clearSession() {
        progressJob?.cancel()
        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        castSession = null
        remoteMediaClient = null
        _currentItem.value = null
        _connectedDevice.value = null
        itemCache.clear()
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
        val isPlaying = client.isPlaying || client.playerState == MediaStatus.PLAYER_STATE_BUFFERING
        val position = client.approximateStreamPosition
        val duration = client.streamDuration

        val isPlaybackFinished = client.playerState == MediaStatus.PLAYER_STATE_IDLE &&
                client.idleReason == MediaStatus.IDLE_REASON_FINISHED

        if (isPlaybackFinished) {
            _currentItem.value = null
            _restoringItemId.value = null
        } else {
            client.mediaInfo?.customData?.optString("itemId")?.takeIf { it.isNotEmpty() }
                ?.let { playingId ->
                    if (playingId != _currentItem.value?.itemId?.toString()) {
                        val fullItem = itemCache[playingId]
                        if (fullItem != null) {
                            Timber.d("Cast track automatically changed! Updating UI to: ${fullItem.name}")
                            _currentItem.value = fullItem
                            _restoringItemId.value = null
                        } else if (_restoringItemId.value != playingId) {
                            Timber.d("Cast item not in cache, requesting restore for: $playingId")
                            _restoringItemId.value = playingId
                        }
                    }
                }
        }

        _playbackState.value = CastPlaybackState(
            isPlaying = isPlaying,
            currentPosition = position,
            duration = duration
        )
        _volume.value = castSession?.volume?.toFloat() ?: 1f
    }

    private fun updateTracks(client: RemoteMediaClient) {
        val mediaInfo = client.mediaInfo ?: return
        val mediaTracks = mediaInfo.mediaTracks ?: emptyList()
        val activeTrackIds = client.mediaStatus?.activeTrackIds?.toList() ?: emptyList()

        Timber.d("Tracks: $mediaTracks")
        Timber.d("Active tracks: $activeTrackIds")

        val subs = mutableListOf<Track>()

        mediaTracks.forEach { castTrack ->
            if (castTrack.type == MediaTrack.TYPE_TEXT) {
                val track = Track(
                    id = castTrack.id.toInt(),
                    label = castTrack.name ?: "external",
                    language = castTrack.language ?: "",
                    codec = castTrack.customData?.getString("mimeType"),
                    selected = activeTrackIds.contains(castTrack.id),
                    supported = true
                )
                subs.add(track)
            }
        }
        _activeTrackIds.value = activeTrackIds
        _subtitleTracks.value = subs

        Timber.d("Subtitle tracks: $subs")
    }

    override fun loadItem(item: PlayerItem, startPosition: Long) {
        itemCache.clear()
        itemCache[item.itemId.toString()] = item

        _currentItem.value = item
        _restoringItemId.value = null
        val client = remoteMediaClient ?: return

        val mediaInfoBuilder = buildMediaInfo(item)

        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfoBuilder)
            .setAutoplay(true)
            .setCurrentTime(startPosition)
            .build()

        client.load(loadRequest)
    }

    private fun buildMediaInfo(item: PlayerItem, selectedAudioIndex: Int = -1): MediaInfo {
        val mediaType =
            if (item.mediaType == PlayerMediaType.EPISODE) MediaMetadata.MEDIA_TYPE_TV_SHOW else MediaMetadata.MEDIA_TYPE_MOVIE
        val mediaMetadata = MediaMetadata(mediaType).apply {
            putString(MediaMetadata.KEY_TITLE, item.name)
            item.seriesName?.let { putString(MediaMetadata.KEY_SERIES_TITLE, it) }
            item.indexNumber?.let { putInt(MediaMetadata.KEY_EPISODE_NUMBER, it) }
            item.parentIndexNumber?.let { putInt(MediaMetadata.KEY_SEASON_NUMBER, it) }
            item.seriesPosterUrl?.let { addImage(WebImage(it.toUri())) }
            item.posterUrl?.let { addImage(WebImage(it.toUri())) }
        }

        val customData = JSONObject().apply {
            put("itemId", item.itemId.toString())
        }

        val baseUrl = jellyfinApi.api.baseUrl
        val accessToken = jellyfinApi.accessToken

        val hlsUrl = "$baseUrl/Videos/${item.itemId}/master.m3u8?" +
                "PlaySessionId=${java.util.UUID.randomUUID()}&" +
                "MediaSourceId=${item.mediaSourceId}&" +
                "VideoCodec=h264&" +
                "AudioCodec=aac,mp3&" +
                "AudioStreamIndex=$selectedAudioIndex&" +
                "SubtitleStreamIndex=-1&" +
                "VideoBitrate=8000000&" +
                "AudioBitrate=192000&" +
                "MaxAudioChannels=6&" +
                "TranscodingMaxAudioChannels=6&" +
                "SegmentContainer=ts&" +
                "MinSegments=1&" +
                "BreakOnNonKeyFrames=True&" +
                "ManifestName=master.m3u8&" +
                "api_key=$accessToken"

        val mediaInfoBuilder = MediaInfo.Builder(hlsUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("application/x-mpegurl")
            .setMetadata(mediaMetadata)
            .setCustomData(customData)

        val castTracks = mutableListOf<MediaTrack>()
        var trackIdCounter = 100L

        // EXTERNAL SUBTITLES
        item.externalSubtitles.forEach { subtitle ->
            var subtitleUrl = subtitle.uri.toString()

            if (!subtitleUrl.contains("api_key=")) {
                subtitleUrl += if (subtitleUrl.contains("?")) "&" else "?"
                subtitleUrl += "api_key=$accessToken"
            }

            var mimeType = subtitle.mimeType
            val data = JSONObject().apply { put("mimeType", subtitle.mimeType) }

            if (subtitleUrl.contains("/Stream.", ignoreCase = true)) {
                subtitleUrl = subtitleUrl.replace(
                    Regex("/Stream\\.[a-z0-9]+", RegexOption.IGNORE_CASE),
                    "/Stream.vtt"
                )
                mimeType = "text/vtt"
            }

            castTracks.add(
                MediaTrack.Builder(trackIdCounter++, MediaTrack.TYPE_TEXT)
                    .setName(subtitle.title + "External")
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentId(subtitleUrl)
                    .setContentType(mimeType)
                    .setLanguage(subtitle.language)
                    .setCustomData(data)
                    .build()
            )
        }

        // 2. INTERNAL SUBTITLES
        // NOTE: This assumes PlayerItem exposes `mediaStreams`.
        // If your property is named differently (like `internalSubtitles`), adjust it below.
        /*
        item.mediaStreams?.filter { it.type == "Subtitle" && !it.isExternal }?.forEach { subStream ->
            val subUrl = "$baseUrl/Videos/${item.itemId}/${item.mediaSourceId}/Subtitles/${subStream.index}/Stream.vtt?api_key=$accessToken"

            castTracks.add(
                MediaTrack.Builder(trackIdCounter++, MediaTrack.TYPE_TEXT)
                    .setName(subStream.title ?: subStream.language ?: "Subtitle ${subStream.index}")
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentId(subUrl)
                    .setContentType("text/vtt")
                    .setLanguage(subStream.language ?: "en")
                    .build()
            )
        }
        */

        if (castTracks.isNotEmpty()) {
            mediaInfoBuilder.setMediaTracks(castTracks)
        }

        return mediaInfoBuilder.build()
    }

    override fun queueNextItem(item: PlayerItem) {
        Timber.d("Queue next: $item")

        itemCache[item.itemId.toString()] = item

        val client = remoteMediaClient ?: return

        val mediaInfo = buildMediaInfo(item)
        val status = client.mediaStatus
        if (status?.queueItems?.lastOrNull()?.media?.contentId == mediaInfo.contentId) return

        val queueItem = MediaQueueItem.Builder(mediaInfo)
            .setAutoplay(true)
            .setPreloadTime(20.0)
            .build()

        val currentItemId = status?.currentItemId ?: MediaQueueItem.INVALID_ITEM_ID
        val queueItems = status?.queueItems ?: emptyList()
        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }

        val nextItemId = if (currentIndex != -1 && currentIndex < queueItems.size - 1) {
            queueItems[currentIndex + 1].itemId
        } else {
            MediaQueueItem.INVALID_ITEM_ID
        }

        client.queueInsertItems(
            arrayOf(queueItem),
            nextItemId,
            null
        )
    }

    override fun queuePreviousItem(item: PlayerItem) {
        Timber.d("Queue previous: $item")

        itemCache[item.itemId.toString()] = item

        val client = remoteMediaClient ?: return

        val mediaInfo = buildMediaInfo(item)
        val status = client.mediaStatus
        if (status?.queueItems?.firstOrNull()?.media?.contentId == mediaInfo.contentId) return

        val queueItem = MediaQueueItem.Builder(mediaInfo)
            .setAutoplay(true)
            .setPreloadTime(20.0)
            .build()

        val currentItemId = client.mediaStatus?.currentItemId ?: MediaQueueItem.INVALID_ITEM_ID
        client.queueInsertItems(
            arrayOf(queueItem),
            currentItemId,
            null
        )
    }

    override fun restoreItem(item: PlayerItem) {
        itemCache[item.itemId.toString()] = item
        _currentItem.value = item
        _restoringItemId.value = null
        remoteMediaClient?.let { updateTracks(it) }
    }

    override fun play() {
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
        remoteMediaClient?.play()
    }

    override fun pause() {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        remoteMediaClient?.pause()
    }

    override fun seekTo(position: Long) {
        val options = MediaSeekOptions.Builder()
            .setPosition(position)
            .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
            .build()

        remoteMediaClient?.seek(options)
    }

    override fun seekToNext() {
        remoteMediaClient?.queueNext(null)
    }

    override fun seekToPrevious() {
        remoteMediaClient?.queuePrev(null)
    }

    override fun setVolume(volume: Float) {
        castSession?.volume = volume.toDouble()
    }

    override fun setSubtitleTrack(track: Track?) {
        val client = remoteMediaClient ?: return
        val activeIds = client.mediaStatus?.activeTrackIds?.toMutableList() ?: mutableListOf()
        val subTrackIds = _subtitleTracks.value.map { it.id.toLong() }
        activeIds.removeAll(subTrackIds)
        if (track != null) {
            activeIds.add(track.id.toLong())
        }
        Timber.d("Active tracks: $activeIds")

        client.setActiveMediaTracks(activeIds.toLongArray())
        updateTracks(client)
    }

    override fun setAudioTrack(track: Track?) {
        val client = remoteMediaClient ?: return
        val item = _currentItem.value ?: return

        val newAudioIndex = track?.id ?: -1

        val status = client.mediaStatus
        val currentPosition = client.approximateStreamPosition

        val newMediaInfo = buildMediaInfo(item, selectedAudioIndex = newAudioIndex)

        val queueItems = status?.queueItems ?: emptyList()
        val currentIndex = queueItems.indexOfFirst { it.itemId == status?.currentItemId }

        if (queueItems.isEmpty() || currentIndex == -1) {
            val loadRequest = MediaLoadRequestData.Builder()
                .setMediaInfo(newMediaInfo)
                .setAutoplay(true)
                .setCurrentTime(currentPosition)
                .build()

            client.load(loadRequest).setResultCallback { result ->
                if (result.status.isSuccess) {
                    Timber.d("Audio track switched successfully via standard load.")
                } else {
                    Timber.e("Failed to switch audio track: ${result.status.statusCode}")
                }
            }
        } else {
            val newQueue = queueItems.mapIndexed { index, queueItem ->
                if (index == currentIndex) {
                    MediaQueueItem.Builder(newMediaInfo)
                        .setAutoplay(true)
                        .setPreloadTime(20.0)
                        .build()
                } else {
                    queueItem.media?.let { media ->
                        MediaQueueItem.Builder(media)
                            .setAutoplay(queueItem.autoplay)
                            .setPreloadTime(queueItem.preloadTime)
                            .build()
                    } ?: queueItem
                }
            }.toTypedArray()

            val repeatMode = status!!.queueRepeatMode

            client.queueLoad(
                newQueue,
                currentIndex,
                repeatMode,
                currentPosition,
                null
            ).setResultCallback { result ->
                if (result.status.isSuccess) {
                    Timber.d("Audio track switched successfully. Queue preserved.")
                } else {
                    Timber.e("Failed to switch audio track in queue: ${result.status.statusCode}")
                }
            }
        }
    }

    override fun stop() {
        remoteMediaClient?.stop()
    }
}

