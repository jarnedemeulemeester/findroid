package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.core.net.toUri
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.player.cast.devices.Chromecast
import dev.jdtech.jellyfin.player.cast.devices.ChromecastH265
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackStatus
import dev.jdtech.jellyfin.player.cast.models.CastPlayerState
import dev.jdtech.jellyfin.player.core.domain.PlaybackManager
import dev.jdtech.jellyfin.player.core.domain.PlaylistManager
import dev.jdtech.jellyfin.player.core.domain.models.PlaybackStatus
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackInfoResponse
import org.jellyfin.sdk.model.serializer.toUUID
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class CastPlayerControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val sessionManager: CastSessionManager,
    private val playbackManager: PlaybackManager,
    private val playlistManager: PlaylistManager
) : CastPlayerController {

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    override val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    private val _playerState = MutableStateFlow(CastPlayerState())
    override val playerState: StateFlow<CastPlayerState> = _playerState.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
    override val subtitleTracks: StateFlow<List<Track>> = _subtitleTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
    override val audioTracks: StateFlow<List<Track>> = _audioTracks.asStateFlow()

    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }

    private var playbackInfoResponse: PlaybackInfoResponse? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    private var castSession: CastSession? = null

    private val queueMutex = Mutex()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private var reportingJob: Job? = null

    private var hasReportedStart = false

    private data class CachedMedia(
        val item: PlayerItem,
        val playbackInfo: PlaybackInfoResponse? = null,
        val subtitleTracks: List<Track> = emptyList(),
        val audioTracks: List<Track> = emptyList()
    )

    private data class BuildMediaResult(
        val mediaInfo: MediaInfo,
        val playbackInfo: PlaybackInfoResponse,
        val subtitleTracks: List<Track>,
        val audioTracks: List<Track>
    )

    private val itemCache = ConcurrentHashMap<UUID, CachedMedia>()

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updatePlaybackStatus()

            // Immediately report status changes (Play/Pause/Seek)
            remoteMediaClient?.let { client ->
                val status = mapPlaybackStatus(client)
                handleReporting(
                    status,
                    client.approximateStreamPosition
                )
            }
        }

        override fun onMetadataUpdated() {
            updatePlaybackStatus()
        }

        override fun onQueueStatusUpdated() {
            updateQueueState()
            restoreSession()
        }

        override fun onPreloadStatusUpdated() {
            super.onPreloadStatusUpdated()
            _playerState.update { it.copy(status = CastPlaybackStatus.BUFFERING) }
        }
    }

    private val castSessionListener = object : Cast.Listener() {
        override fun onVolumeChanged() {
            val session = castSession ?: return
            _playerState.update {
                it.copy(
                    volume = session.volume.toFloat(),
                    isMuted = session.isMute
                )
            }
        }

        override fun onStandbyStateChanged(standbyState: Int) {
            if (standbyState == Cast.STANDBY_STATE_YES) {
                stop()
            }
        }
    }

    init {
        scope.launch {
            sessionManager.connectionState.collectLatest { state ->
                val session = castContext.sessionManager.currentCastSession
                if (session != castSession) {
                    castSession?.removeCastListener(castSessionListener)
                    castSession = session
                    castSession?.addCastListener(castSessionListener)

                    _playerState.update {
                        it.copy(
                            volume = session?.volume?.toFloat() ?: 1f,
                            isMuted = session?.isMute ?: false
                        )
                    }
                }

                val client = session?.remoteMediaClient
                if (client != remoteMediaClient) {
                    remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
                    remoteMediaClient = client
                    client?.let {
                        it.registerCallback(remoteMediaClientCallback)
                        updatePlaybackStatus()
                        restoreSession()
                        startTracking()
                        startReporting()
                    }
                }

                if (state == CastConnectionState.DISCONNECTED || session == null) {
                    clearSession()
                }
            }
        }
    }

    private fun clearSession() {
        val currentItem = _currentItem.value
        val currentPlaybackInfo = playbackInfoResponse
        val currentState = _playerState.value
        val reportedStart = hasReportedStart

        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        remoteMediaClient = null
        castSession?.removeCastListener(castSessionListener)
        castSession = null
        progressJob?.cancel()
        reportingJob?.cancel()

        scope.launch {
            if (currentItem != null && reportedStart) {
                reportPlaybackStop(
                    item = currentItem,
                    positionMs = currentState.currentPosition,
                    durationMs = currentState.duration,
                    playbackInfo = currentPlaybackInfo
                )
            }
            _currentItem.value = null
            hasReportedStart = false
            itemCache.clear()
        }
    }

    private fun startTracking() {
        progressJob?.cancel()
        val status = _playerState.value.status
        if (status == CastPlaybackStatus.IDLE || status == CastPlaybackStatus.ENDED) return

        progressJob = scope.launch {
            while (isActive) {
                val client = remoteMediaClient
                if (client != null) {
                    // Update only position periodically for UI
                    _playerState.update {
                        it.copy(
                            currentPosition = client.approximateStreamPosition,
                        )
                    }
                }
                delay(1000.milliseconds)
            }
        }
    }

    private fun startReporting() {
        reportingJob?.cancel()
        val status = _playerState.value.status
        if (status == CastPlaybackStatus.IDLE || status == CastPlaybackStatus.ENDED) return

        reportingJob = scope.launch {
            while (isActive) {
                val client = remoteMediaClient
                if (client != null && hasReportedStart) {
                    handleReporting(
                        status = mapPlaybackStatus(client),
                        position = client.approximateStreamPosition,
                    )
                }
                delay(10000.milliseconds)
            }
        }
    }

    private fun mapPlaybackStatus(client: RemoteMediaClient): CastPlaybackStatus {
        return when (client.playerState) {
            MediaStatus.PLAYER_STATE_BUFFERING -> CastPlaybackStatus.BUFFERING
            MediaStatus.PLAYER_STATE_PLAYING -> CastPlaybackStatus.PLAYING
            MediaStatus.PLAYER_STATE_PAUSED -> CastPlaybackStatus.PAUSED
            MediaStatus.PLAYER_STATE_IDLE -> {
                if (client.idleReason == MediaStatus.IDLE_REASON_FINISHED) CastPlaybackStatus.ENDED
                else CastPlaybackStatus.IDLE
            }

            else -> CastPlaybackStatus.IDLE
        }
    }

    private fun updatePlaybackStatus() {
        val client = remoteMediaClient ?: return
        val playbackStatus = mapPlaybackStatus(client)
        val currentPosition = client.approximateStreamPosition
        val duration = client.streamDuration

        // Capture current values for reporting before we update anything
        val currentItem = _currentItem.value
        val currentPlaybackInfo = playbackInfoResponse
        val currentState = _playerState.value

        if (playbackStatus == CastPlaybackStatus.ENDED) {
            if (currentItem != null && hasReportedStart) {
                scope.launch {
                    reportPlaybackStop(
                        item = currentItem,
                        positionMs = if (currentPosition > currentState.duration) currentPosition else currentState.duration,
                        durationMs = currentState.duration,
                        playbackInfo = currentPlaybackInfo
                    )
                }
                _currentItem.value = null
                hasReportedStart = false
            }
        } else {
            client.mediaInfo?.customData?.optString("itemId")?.takeIf { it.isNotEmpty() }
                ?.let { playingId ->
                    if (playingId != currentItem?.itemId?.toString()) {
                        val cached = itemCache[playingId.toUUID()]
                        if (cached != null) {
                            Timber.d("Cast track automatically changed! Updating UI to: ${cached.item.name}")
                            if (currentItem != null && hasReportedStart) {
                                scope.launch {
                                    reportPlaybackStop(
                                        item = currentItem,
                                        positionMs = currentState.currentPosition,
                                        durationMs = currentState.duration,
                                        playbackInfo = currentPlaybackInfo
                                    )
                                }
                            }

                            playbackInfoResponse = cached.playbackInfo
                            _subtitleTracks.value = cached.subtitleTracks
                            _audioTracks.value = cached.audioTracks
                            _currentItem.value = cached.item
                            hasReportedStart = false

                            manageQueue(cached.item)
                        } else {
                            Timber.d("Cast item not in cache, requesting restore for: $playingId")
                            restoreRemoteItem(playingId, isCurrent = true)
                        }
                    }
                }
        }

        val previousStatus = currentState.status
        _playerState.update {
            it.copy(
                status = playbackStatus,
                duration = if (playbackStatus == CastPlaybackStatus.ENDED) currentState.duration else duration,
                currentPosition = if (playbackStatus == CastPlaybackStatus.ENDED) currentState.duration else currentPosition,
            )
        }

        // Restart jobs if status changed from idle to active or vice versa
        if (playbackStatus != previousStatus) {
            if (playbackStatus == CastPlaybackStatus.IDLE || playbackStatus == CastPlaybackStatus.ENDED) {
                progressJob?.cancel()
                reportingJob?.cancel()
            } else if (previousStatus == CastPlaybackStatus.IDLE || previousStatus == CastPlaybackStatus.ENDED) {
                startTracking()
                startReporting()
            }
        }
    }

    private fun handleReporting(
        status: CastPlaybackStatus,
        position: Long,
    ) {
        if (status == CastPlaybackStatus.ENDED || status == CastPlaybackStatus.IDLE) return
        val currentItem = _currentItem.value ?: return
        val playbackInfo = playbackInfoResponse
        val mediaSource = playbackInfo?.mediaSources?.firstOrNull()

        val playMethod = when {
            mediaSource?.supportsDirectPlay ?: false -> PlayMethod.DIRECT_PLAY
            mediaSource?.supportsDirectStream ?: false -> PlayMethod.DIRECT_STREAM
            else -> PlayMethod.TRANSCODE
        }

        val playbackReportStatus = PlaybackStatus(
            itemId = currentItem.itemId,
            positionMs = position,
            isPaused = status != CastPlaybackStatus.PLAYING,
            playMethod = playMethod,
            mediaSourceId = mediaSource?.id,
            playSessionId = playbackInfo?.playSessionId
        )

        // Progress Reporting
        if (hasReportedStart) {
            scope.launch {
                playbackManager.reportProgress(playbackReportStatus)
            }
        } else if (status == CastPlaybackStatus.PLAYING) {
            scope.launch {
                playbackManager.reportStart(playbackReportStatus)
                hasReportedStart = true
            }
        }
    }

    private suspend fun reportPlaybackStop(
        item: PlayerItem? = _currentItem.value,
        positionMs: Long = _playerState.value.currentPosition,
        durationMs: Long = _playerState.value.duration,
        playbackInfo: PlaybackInfoResponse? = playbackInfoResponse
    ) {
        val targetItem = item ?: return

        playbackManager.reportStop(
            PlaybackStatus(
                itemId = targetItem.itemId,
                positionMs = positionMs,
                durationMs = durationMs,
                mediaSourceId = playbackInfo?.mediaSources?.firstOrNull()?.id,
                playSessionId = playbackInfo?.playSessionId
            )
        )
    }

    private fun restoreSession() {
        val client = remoteMediaClient ?: return
        val status = client.mediaStatus ?: return
        val currentItemId = status.currentItemId

        status.queueItems.forEach { queueItem ->
            val itemIdStr = queueItem.media?.customData?.optString("itemId") ?: return@forEach
            if (!itemCache.containsKey(itemIdStr.toUUID())) {
                restoreRemoteItem(itemIdStr, isCurrent = (queueItem.itemId == currentItemId))
            }
        }
    }

    private fun restoreRemoteItem(itemIdStr: String, isCurrent: Boolean = false) {
        scope.launch {
            try {
                val itemId = UUID.fromString(itemIdStr)
                val userId = jellyfinApi.userId
                val findroidItem = jellyfinApi.userLibraryApi.getItem(itemId, userId).content
                val itemKind = when (findroidItem.type) {
                    BaseItemKind.MOVIE -> BaseItemKind.MOVIE
                    BaseItemKind.EPISODE -> BaseItemKind.EPISODE
                    else -> return@launch
                }
                val playerItem = playlistManager.getInitialItem(
                    itemId = itemId,
                    itemKind = itemKind,
                    mediaSourceIndex = null,
                    startFromBeginning = false
                )
                if (playerItem != null) {
                    if (isCurrent) {
                        val playbackInfo = getPlaybackInfo(playerItem, null)
                        itemCache[playerItem.itemId] = CachedMedia(
                            item = playerItem,
                            playbackInfo = playbackInfo
                        )
                        playbackInfoResponse = playbackInfo
                        restoreItem(playerItem)
                        manageQueue(playerItem)
                    } else {
                        itemCache[playerItem.itemId] = CachedMedia(playerItem)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore item $itemIdStr")
            }
        }
    }

    private suspend fun getPlaybackInfo(
        item: PlayerItem,
        audioStreamIndex: Int?
    ): PlaybackInfoResponse? {
        val userId = jellyfinApi.userId
        val profile = if (true /* logic for Chromecast 4k */) {
            Chromecast.deviceProfile
        } else {
            ChromecastH265.deviceProfile
        }

        return try {
            jellyfinApi.mediaInfoApi.getPostedPlaybackInfo(
                item.itemId,
                PlaybackInfoDto(
                    userId = userId,
                    deviceProfile = profile,
                    audioStreamIndex = audioStreamIndex,
                    enableDirectPlay = audioStreamIndex == null,
                    enableDirectStream = true,
                    enableTranscoding = true,
                    allowAudioStreamCopy = true,
                    allowVideoStreamCopy = true
                )
            ).content
        } catch (e: Exception) {
            Timber.e(e, "Failed to get playback info")
            null
        }
    }

    private suspend fun buildMediaInfo(
        item: PlayerItem,
        audioStreamIndex: Int? = null
    ): BuildMediaResult? {
        val baseUrl = jellyfinApi.api.baseUrl

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

        val playbackInfo = getPlaybackInfo(item, audioStreamIndex) ?: return null
        val mediaSource = playbackInfo.mediaSources.firstOrNull() ?: return null

        val (streamUrlOriginal, contentType) = if (mediaSource.supportsDirectPlay) {
            val url =
                baseUrl + "/Videos/${item.itemId}/stream?static=true&MediaSourceId=${mediaSource.id}"
            val mimeType = mediaSource.container?.let { if (it.contains("/")) it else "video/$it" }
                ?: "video/mp4"
            url to mimeType
        } else {
            val url = baseUrl + (mediaSource.transcodingUrl
                ?: "/Videos/${item.itemId}/stream?MediaSourceId=${mediaSource.id}")
            url to "application/x-mpegurl"
        }

        val streamUrl = if (audioStreamIndex != null) {
            if (streamUrlOriginal.contains("AudioStreamIndex=")) {
                streamUrlOriginal.replace(
                    Regex("AudioStreamIndex=\\d+"),
                    "AudioStreamIndex=$audioStreamIndex"
                )
            } else {
                "$streamUrlOriginal&AudioStreamIndex=$audioStreamIndex"
            }
        } else {
            streamUrlOriginal
        }

        Timber.d("Video url: $streamUrl")
        Timber.d("PlaySessionId (url): ${playbackInfo.playSessionId}")
        Timber.d("Bitrate: ${mediaSource.bitrate}")

        val mediaInfoBuilder = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(mediaMetadata)
            .setCustomData(customData)

        val castTracks = mutableListOf<MediaTrack>()
        val subtitles = mutableListOf<Track>()
        val audio = mutableListOf<Track>()

        mediaSource.mediaStreams?.forEach { stream ->

            // Subtitle
            if (stream.type == MediaStreamType.SUBTITLE) {
                val trackId = (stream.index + 100).toLong()
                val trackUrl = baseUrl + stream.deliveryUrl

                val builder = MediaTrack.Builder(trackId, MediaTrack.TYPE_TEXT)
                    .setContentId(trackUrl)
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentType("text/vtt")
                    .setLanguage(stream.language)
                    .setName(stream.title ?: stream.displayTitle ?: "Track ${stream.index}")

                castTracks.add(builder.build())

                val track = Track(
                    id = trackId.toInt(),
                    label = stream.title,
                    language = stream.language,
                    codec = stream.codec,
                    selected = stream.isDefault,
                    supported = true,
                    isExternal = stream.isExternal,
                    isForced = stream.isForced,
                    isHearingImpaired = stream.isHearingImpaired,
                )

                subtitles.add(track)

                Timber.d("Subtitle track: $stream")

            }

            // Audio
            else if (stream.type == MediaStreamType.AUDIO) {
                val track = Track(
                    id = stream.index,
                    label = stream.title,
                    language = stream.language,
                    codec = stream.codec,
                    selected = stream.isDefault,
                    supported = true,
                    isExternal = stream.isExternal,
                    isForced = stream.isForced,
                    isHearingImpaired = stream.isHearingImpaired,
                )

                audio.add(track)

                Timber.d("Audio track: $stream")
            }
        }

        if (castTracks.isNotEmpty()) {
            mediaInfoBuilder.setMediaTracks(castTracks)
        }

        return BuildMediaResult(
            mediaInfo = mediaInfoBuilder.build(),
            playbackInfo = playbackInfo,
            subtitleTracks = subtitles,
            audioTracks = audio
        )
    }

    override fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        scope.launch {
            val initialItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = BaseItemKind.fromName(itemKind),
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning
            )
            if (initialItem != null) {
                loadItem(initialItem, if (startFromBeginning) 0L else initialItem.playbackPosition)
            }
        }
    }

    private fun loadItem(item: PlayerItem, startPosition: Long) {
        scope.launch {
            itemCache.clear()
            val result = buildMediaInfo(item) ?: return@launch
            itemCache[item.itemId] = CachedMedia(
                item = item,
                playbackInfo = result.playbackInfo,
                subtitleTracks = result.subtitleTracks,
                audioTracks = result.audioTracks
            )

            val client = remoteMediaClient ?: return@launch

            val loadRequest = MediaLoadRequestData.Builder()
                .setMediaInfo(result.mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(startPosition)
                .build()

            hasReportedStart = false
            client.load(loadRequest)

            playbackInfoResponse = result.playbackInfo
            _subtitleTracks.value = result.subtitleTracks
            _audioTracks.value = result.audioTracks
            _currentItem.value = item
            manageQueue(item)
        }
    }

    private fun restoreItem(item: PlayerItem) {
        itemCache[item.itemId] = CachedMedia(item)
        _currentItem.value = item
    }

    private fun manageQueue(item: PlayerItem) {
        playlistManager.setCurrentMediaItemIndex(item.itemId)

        scope.launch {
            playlistManager.getNextPlayerItem()?.let { queueNextItem(it) }
        }
        scope.launch {
            playlistManager.getPreviousPlayerItem()?.let { queuePreviousItem(it) }
        }
    }

    private fun updateQueueState() {
        val status = remoteMediaClient?.mediaStatus ?: return
        val queueItems = status.queueItems

        val currentIndex = queueItems.indexOfFirst { it.itemId == status.currentItemId }

        val hasNext = currentIndex != -1 && currentIndex < queueItems.size - 1
        val hasPrevious = currentIndex > 0

        _playerState.update { it.copy(hasNextItem = hasNext, hasPreviousItem = hasPrevious) }
    }

    fun queueNextItem(item: PlayerItem) {
        scope.launch {
            val result = buildMediaInfo(item) ?: return@launch
            itemCache[item.itemId] = CachedMedia(
                item = item,
                playbackInfo = result.playbackInfo,
                subtitleTracks = result.subtitleTracks,
                audioTracks = result.audioTracks
            )

            queueMutex.withLock {
                val client = remoteMediaClient ?: return@withLock
                val status = client.mediaStatus ?: return@withLock

                val isAlreadyInQueue = status.queueItems.any { queueItem ->
                    val existingItemId = queueItem.media?.customData?.optString("itemId")
                    existingItemId == item.itemId.toString()
                }

                if (isAlreadyInQueue) {
                    Timber.d("Item ${item.name} already in Cast queue.")
                    return@withLock
                }

                val queueItem = MediaQueueItem.Builder(result.mediaInfo)
                    .setAutoplay(true)
                    .setPreloadTime(20.0)
                    .build()

                val currentItemId = status.currentItemId
                val queueItems = status.queueItems
                val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }

                val nextItemId = if (currentIndex != -1 && currentIndex < queueItems.size - 1) {
                    queueItems[currentIndex + 1].itemId
                } else {
                    MediaQueueItem.INVALID_ITEM_ID
                }

                client.queueInsertItems(arrayOf(queueItem), nextItemId, null)
                updateQueueState()
            }
        }
    }

    fun queuePreviousItem(item: PlayerItem) {
        scope.launch {
            val result = buildMediaInfo(item) ?: return@launch
            itemCache[item.itemId] = CachedMedia(
                item = item,
                playbackInfo = result.playbackInfo,
                subtitleTracks = result.subtitleTracks,
                audioTracks = result.audioTracks
            )

            queueMutex.withLock {
                val client = remoteMediaClient ?: return@withLock
                val status = client.mediaStatus ?: return@withLock

                val isAlreadyInQueue = status.queueItems.any { queueItem ->
                    val existingItemId = queueItem.media?.customData?.optString("itemId")
                    existingItemId == item.itemId.toString()
                }

                if (isAlreadyInQueue) {
                    Timber.d("Item ${item.name} already in Cast queue.")
                    return@withLock
                }

                val queueItem = MediaQueueItem.Builder(result.mediaInfo)
                    .setAutoplay(true)
                    .setPreloadTime(20.0)
                    .build()

                val currentItemId = status.currentItemId

                client.queueInsertItems(arrayOf(queueItem), currentItemId, null)
                updateQueueState()
            }
        }
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
        client.setActiveMediaTracks(activeIds.toLongArray()).setResultCallback { result ->
            if (result.status.isSuccess) {
                val activeTrackIds = client.mediaStatus?.activeTrackIds?.toList() ?: emptyList()

                _subtitleTracks.update { tracks ->
                    tracks.map {
                        it.copy(selected = activeTrackIds.contains(it.id.toLong()))
                    }
                }

                Timber.d("Selected subtitle track: $track")
            }
        }
    }

    override fun setAudioTrack(track: Track?, itemId: UUID?) {
        if (itemId == null || track == null) return
        scope.launch {
            val client = remoteMediaClient ?: return@launch
            val cachedMedia = itemCache[itemId] ?: return@launch
            val currentPositionMs = client.approximateStreamPosition

            // Request new playback info from Jellyfin with the selected audio track index
            val result = buildMediaInfo(cachedMedia.item, track.id) ?: return@launch

            // Update the current item in the Cast queue
            val loadRequest = MediaLoadRequestData.Builder()
                .setMediaInfo(result.mediaInfo)
                .setCurrentTime(currentPositionMs)
                .setAutoplay(true)
                .build()

            client.load(loadRequest).setResultCallback { callbackResult ->
                if (callbackResult.status.isSuccess) {
                    itemCache.clear()

                    val updatedMedia = cachedMedia.copy(
                        playbackInfo = result.playbackInfo,
                        subtitleTracks = result.subtitleTracks,
                        audioTracks = result.audioTracks
                    )

                    itemCache[itemId] = updatedMedia

                    playbackInfoResponse = result.playbackInfo
                    _subtitleTracks.value = result.subtitleTracks
                    _audioTracks.value = result.audioTracks

                    Timber.d("Selected audio track: $track")

                    manageQueue(cachedMedia.item)
                }
            }
        }
    }

    override fun stop() {
        remoteMediaClient?.stop()
    }
}
