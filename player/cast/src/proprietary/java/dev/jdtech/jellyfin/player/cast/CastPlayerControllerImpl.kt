package dev.jdtech.jellyfin.player.cast

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.MediaError
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
import dev.jdtech.jellyfin.player.cast.models.CastMediaItem
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackStatus
import dev.jdtech.jellyfin.player.cast.models.CastPlayerState
import dev.jdtech.jellyfin.player.core.domain.PlaybackManager
import dev.jdtech.jellyfin.player.core.domain.PlaylistManager
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.PlayerMediaType
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.player.core.domain.utils.NetworkSpeedUtils.measureNetworkSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
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

@Singleton
class CastPlayerControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val sessionManager: CastSessionManager,
    private val playbackManager: PlaybackManager,
    private val playlistManager: PlaylistManager
) : CastPlayerController {

    private val _currentItem = MutableStateFlow<CastMediaItem?>(null)
    override val currentItem: StateFlow<CastMediaItem?> = _currentItem.asStateFlow()

    private val _playerState = MutableStateFlow(CastPlayerState())
    override val playerState: StateFlow<CastPlayerState> = _playerState.asStateFlow()

    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }

    private var remoteMediaClient: RemoteMediaClient? = null
    private var castSession: CastSession? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val queueMutex = Mutex()

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    var isSessionRestored = false
    private var isReporting = false
    private var lastActiveItemId: Int = MediaQueueItem.INVALID_ITEM_ID

    private var maxBitrate: Int? = null
    private lateinit var playMethod: PlayMethod
    private var audioStreamIndex: Int? = null
    private var subtitleStreamIndex: Int? = null

    private data class BuildMediaResult(
        val mediaInfo: MediaInfo,
        val playbackInfo: PlaybackInfoResponse,
        val subtitleTracks: List<Track>,
        val audioTracks: List<Track>
    )

    private val itemCache = ConcurrentHashMap<UUID, CastMediaItem>()
    private val itemDuration = ConcurrentHashMap<UUID, Long>()

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updatePlaybackStatus()

            if (!isSessionRestored && remoteMediaClient?.mediaStatus != null) {
                restoreSession()
            }
        }

        override fun onMetadataUpdated() {
            super.onMetadataUpdated()
            val client = remoteMediaClient ?: return
            val itemIdStr = client.currentItem?.media?.customData?.optString("itemId") ?: return
            val cachedItem = itemCache[itemIdStr.toUUID()] ?: return

            _currentItem.value = cachedItem

            itemDuration[itemIdStr.toUUID()] = client.streamDuration

            manageQueue(itemIdStr.toUUID())
        }

        override fun onQueueStatusUpdated() {
            val client = remoteMediaClient ?: return
            val currentActiveItemId = client.mediaStatus?.currentItemId ?: return

            if (currentActiveItemId != lastActiveItemId) {
                if (lastActiveItemId != MediaQueueItem.INVALID_ITEM_ID) {
                    val previousItem = client.mediaStatus?.getQueueItemById(lastActiveItemId)
                    val previousItemIdStr = previousItem?.media?.customData?.optString("itemId")

                    if (previousItemIdStr != null) {
                        stopReporting(previousItemIdStr.toUUID())
                        if (currentActiveItemId != MediaQueueItem.INVALID_ITEM_ID) {
                            startReporting()
                        }
                    }
                }

                lastActiveItemId = currentActiveItemId
            }
        }

        override fun onMediaError(p0: MediaError) {
            super.onMediaError(p0)
            Timber.e("Media Error: $p0")
        }
    }

    private val playbackReportingCallback =
        RemoteMediaClient.ProgressListener { progressMs, _ ->
            reportPlaybackProgressAndState(progressMs)
        }

    private val remoteMediaClientProgressListener =
        RemoteMediaClient.ProgressListener { progressMs, durationMs ->
            _playerState.update {
                it.copy(
                    currentPosition = progressMs,
                    duration = durationMs
                )
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
            sessionManager.connectionState.collect { state ->
                val session = if (state == CastConnectionState.CONNECTED) {
                    castContext.sessionManager.currentCastSession
                } else null

                if (state == CastConnectionState.CONNECTED && session != null) {
                    if (session != castSession) {
                        // Cleanup previous session
                        if (castSession != null) {
                            castSession?.removeCastListener(castSessionListener)
                            remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
                            remoteMediaClient?.removeProgressListener(
                                remoteMediaClientProgressListener
                            )
                            remoteMediaClient?.removeProgressListener(playbackReportingCallback)
                        }

                        // Setup new session
                        castSession = session
                        castSession?.addCastListener(castSessionListener)

                        remoteMediaClient = session.remoteMediaClient
                        remoteMediaClient?.let { client ->
                            client.registerCallback(remoteMediaClientCallback)
                            isSessionRestored = false
                            restoreSession()
                        }

                        _playerState.update {
                            it.copy(
                                volume = session.volume.toFloat(),
                                isMuted = session.isMute
                            )
                        }

                        if (maxBitrate == null) {
                            scope.launch {
                                maxBitrate = measureNetworkSpeed(jellyfinApi)
                            }
                        }
                    }
                } else if (state == CastConnectionState.DISCONNECTED) {
                    clearSession()
                }
            }
        }
    }

    private fun clearSession() {
        Timber.d("Clear session")

        stopReporting()

        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        remoteMediaClient?.removeProgressListener(remoteMediaClientProgressListener)
        remoteMediaClient = null

        castSession?.removeCastListener(castSessionListener)
        castSession = null

        _currentItem.value = null
        isSessionRestored = false
    }

    private fun mapPlaybackStatus(client: RemoteMediaClient): CastPlaybackStatus {
        return when (client.playerState) {
            MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.PLAYER_STATE_LOADING -> CastPlaybackStatus.BUFFERING
            MediaStatus.PLAYER_STATE_PLAYING -> CastPlaybackStatus.PLAYING
            MediaStatus.PLAYER_STATE_PAUSED -> CastPlaybackStatus.PAUSED
            MediaStatus.PLAYER_STATE_IDLE -> {
                when (client.idleReason) {
                    MediaStatus.IDLE_REASON_FINISHED -> CastPlaybackStatus.ENDED
                    MediaStatus.IDLE_REASON_ERROR -> CastPlaybackStatus.ERROR
                    else -> CastPlaybackStatus.IDLE
                }
            }

            else -> CastPlaybackStatus.IDLE
        }
    }

    private fun updatePlaybackStatus() {
        val client = remoteMediaClient ?: return
        val playbackStatus = mapPlaybackStatus(client)

        _playerState.update { it.copy(status = playbackStatus) }

        when (playbackStatus) {
            CastPlaybackStatus.ENDED, CastPlaybackStatus.IDLE -> {
                val itemId = _currentItem.value?.item?.itemId
                stopReporting(itemId)
                _currentItem.value = null
                stopReporting()
                remoteMediaClient?.removeProgressListener(remoteMediaClientProgressListener)
            }

            CastPlaybackStatus.PLAYING -> {
                startReporting()
                remoteMediaClient?.addProgressListener(remoteMediaClientProgressListener, 1000L)
            }

            CastPlaybackStatus.PAUSED -> {
                if (isReporting) {
                    reportPlaybackProgressAndState()
                    remoteMediaClient?.removeProgressListener(playbackReportingCallback)
                }
                remoteMediaClient?.removeProgressListener(remoteMediaClientProgressListener)
            }

            else -> {
                if (isReporting) {
                    reportPlaybackProgressAndState()
                }
            }
        }
    }

    private fun reportPlaybackProgressAndState(
        positionMs: Long? = null
    ) {
        val client = remoteMediaClient ?: return
        val currentItem = _currentItem.value ?: return
        val playbackInfo = currentItem.playbackInfo

        scope.launch {
            playbackManager.reportProgress(
                itemId = currentItem.item.itemId,
                positionMs = positionMs ?: client.approximateStreamPosition,
                isPaused = !client.isPlaying,
                playMethod = playMethod,
                mediaSourceId = playbackInfo?.mediaSources?.firstOrNull()?.id,
                playSessionId = playbackInfo?.playSessionId
            )
        }
    }

    private fun startReporting() {
        if (!isReporting) {
            val client = remoteMediaClient ?: return
            val currentItem = _currentItem.value ?: return
            val playbackInfo = currentItem.playbackInfo
            val mediaSource = playbackInfo?.mediaSources?.firstOrNull()
            val startPositionMs = if (client.approximateStreamPosition == 0L) {
                    currentItem.item.playbackPosition
                } else {
                    client.approximateStreamPosition
                }

            playMethod = when {
                mediaSource?.supportsDirectPlay ?: false -> PlayMethod.DIRECT_PLAY
                mediaSource?.supportsDirectStream ?: false -> PlayMethod.DIRECT_STREAM
                else -> PlayMethod.TRANSCODE
            }

            scope.launch {
                playbackManager.reportStart(
                    itemId = currentItem.item.itemId,
                    positionMs = startPositionMs,
                    playMethod = playMethod,
                    mediaSourceId = playbackInfo?.mediaSources?.firstOrNull()?.id,
                    playSessionId = playbackInfo?.playSessionId
                )
            }

            isReporting = true
        } else {
            reportPlaybackProgressAndState()
        }

        remoteMediaClient?.addProgressListener(playbackReportingCallback, 10000L)
    }

    private fun stopReporting(
        itemId: UUID? = null
    ) {
        if (!isReporting) return

        val targetItemId = itemId ?: _currentItem.value?.item?.itemId ?: return
        val currentItem = itemCache[targetItemId] ?: return
        val playbackInfo = currentItem.playbackInfo
        val playerState = _playerState.value

        scope.launch {
            playbackManager.reportStop(
                itemId = targetItemId,
                positionMs = playerState.currentPosition,
                durationMs = playerState.duration,
                mediaSourceId = playbackInfo?.mediaSources?.firstOrNull()?.id,
                playSessionId = playbackInfo?.playSessionId
            )
        }

        if (itemId != null) {
            itemCache.remove(itemId)
            itemDuration.remove(itemId)
        } else {
            itemCache.clear()
            itemDuration.clear()
        }

        remoteMediaClient?.removeProgressListener(playbackReportingCallback)
        isReporting = false
    }

    private fun restoreSession() {
        val client = remoteMediaClient ?: return
        val status = client.mediaStatus ?: return

        if (status.playerState == MediaStatus.PLAYER_STATE_IDLE || isSessionRestored) return

        Timber.d("Restoring Session")
        isSessionRestored = true

        val currentItemId = status.currentItemId

        val currentQueueItem = status.getQueueItemById(currentItemId)
        val streamUrl = currentQueueItem?.media?.contentId
        Timber.d("Stream Url: $streamUrl")
        audioStreamIndex = streamUrl?.let { url ->
            Regex("AudioStreamIndex=(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull()
        }
        subtitleStreamIndex = status.activeTrackIds?.firstOrNull()?.toInt()

        status.queueItems.forEach { queueItem ->
            val customData = queueItem.media?.customData ?: return@forEach
            val itemIdStr = customData.optString("itemId") ?: return@forEach
            val playbackInfoStr = customData.optString("playbackInfo")
            val playbackInfo = if (!playbackInfoStr.isNullOrEmpty()) {
                try {
                    json.decodeFromString<PlaybackInfoResponse>(playbackInfoStr)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode playbackInfo")
                    null
                }
            } else null

            if (!itemCache.containsKey(itemIdStr.toUUID())) {
                restoreRemoteItem(
                    itemIdStr,
                    playbackInfo,
                    isCurrent = (queueItem.itemId == currentItemId)
                )
            }
        }

        updatePlaybackStatus()

        currentQueueItem?.media?.customData?.optString("itemId")?.let {
            Timber.d("Managing queue")
            manageQueue(it.toUUID())
        }

        Timber.d("Restored Session")
        Timber.d("Audio stream index: $audioStreamIndex")
        Timber.d("Subtitle stream index: $subtitleStreamIndex")
    }

    private fun restoreRemoteItem(
        itemIdStr: String,
        playbackInfo: PlaybackInfoResponse?,
        isCurrent: Boolean = false
    ) {
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
                    val mediaSource = playbackInfo?.mediaSources?.firstOrNull()

                    val (_, subtitles, audio) = if (mediaSource != null) {
                        getTracks(mediaSource)
                    } else {
                        Triple(emptyList(), emptyList(), emptyList())
                    }

                    val castItem = CastMediaItem(
                        item = playerItem,
                        playbackInfo = playbackInfo,
                        subtitleTracks = subtitles,
                        audioTracks = audio
                    )

                    itemCache[playerItem.itemId] = castItem

                    if (isCurrent) {
                        _currentItem.value = castItem
                        manageQueue(itemIdStr.toUUID())
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
        val connectedDevice = sessionManager.connectedDevice.value
        val profile = if (connectedDevice?.supportsH265 == true) {
            ChromecastH265.deviceProfile
        } else {
            Chromecast.deviceProfile
        }

        return try {
            jellyfinApi.mediaInfoApi.getPostedPlaybackInfo(
                item.itemId,
                PlaybackInfoDto(
                    userId = userId,
                    deviceProfile = profile,
                    maxStreamingBitrate = maxBitrate,
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

    private fun Uri.toCastOptimizeImageUri(
        mediaType: PlayerMediaType = PlayerMediaType.MOVIE,
        isBackdrop: Boolean = false
    ): Uri {
        val quality = 70

        val (targetWidthPx, targetHeightPx) = if (mediaType == PlayerMediaType.EPISODE || isBackdrop) {
            1280 to 720
        } else {
            480 to 720
        }

        return this.buildUpon()
            .appendQueryParameter("fillWidth", targetWidthPx.toString())
            .appendQueryParameter("fillHeight", targetHeightPx.toString())
            .appendQueryParameter("quality", quality.toString())
            .build()
    }

    private suspend fun buildMediaInfo(item: PlayerItem): BuildMediaResult? {
        val baseUrl = jellyfinApi.api.baseUrl

        val mediaType =
            if (item.mediaType == PlayerMediaType.EPISODE) MediaMetadata.MEDIA_TYPE_TV_SHOW else MediaMetadata.MEDIA_TYPE_MOVIE
        val mediaMetadata = MediaMetadata(mediaType).apply {
            putString(MediaMetadata.KEY_TITLE, item.name)
            item.seriesName?.let { putString(MediaMetadata.KEY_SERIES_TITLE, it) }

            item.indexNumber?.let { putInt(MediaMetadata.KEY_EPISODE_NUMBER, it) }
            item.parentIndexNumber?.let { putInt(MediaMetadata.KEY_SEASON_NUMBER, it) }

            item.images.showPrimary?.let { addImage(WebImage(it.toCastOptimizeImageUri())) }
            item.images.showBackdrop?.let { addImage(WebImage(it.toCastOptimizeImageUri(isBackdrop = true))) }

            item.images.primary?.let { addImage(WebImage(it.toCastOptimizeImageUri(item.mediaType))) }
            item.images.backdrop?.let {
                addImage(
                    WebImage(
                        it.toCastOptimizeImageUri(
                            item.mediaType,
                            isBackdrop = true
                        )
                    )
                )
            }
        }

        val playbackInfo = getPlaybackInfo(item, audioStreamIndex) ?: return null

        val customData = JSONObject().apply {
            put("itemId", item.itemId.toString())
            put("playbackInfo", json.encodeToString(playbackInfo))
        }

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

        val mediaInfoBuilder = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(mediaMetadata)
            .setCustomData(customData)

        val (castTracks, subtitles, audio) = getTracks(mediaSource)

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

    private fun getTracks(mediaSource: MediaSourceInfo): Triple<List<MediaTrack>, List<Track>, List<Track>> {
        val baseUrl = jellyfinApi.api.baseUrl

        val castTracks = mutableListOf<MediaTrack>()
        val subtitles = mutableListOf<Track>()
        val audio = mutableListOf<Track>()

        mediaSource.mediaStreams?.forEach { stream ->

            // Subtitle
            if (stream.type == MediaStreamType.SUBTITLE) {
                val trackId = (stream.index + 100)
                val trackUrl = baseUrl + stream.deliveryUrl

                if (subtitleStreamIndex == null && stream.isDefault) {
                    subtitleStreamIndex = trackId
                }

                val builder = MediaTrack.Builder(trackId.toLong(), MediaTrack.TYPE_TEXT)
                    .setContentId(trackUrl)
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentType("text/vtt")
                    .setLanguage(stream.language)
                    .setName(stream.title ?: stream.displayTitle ?: "Track ${stream.index}")

                castTracks.add(builder.build())

                val track = Track(
                    id = trackId,
                    label = stream.title,
                    language = stream.language,
                    codec = stream.codec,
                    selected = if (subtitleStreamIndex != null) trackId == subtitleStreamIndex else stream.isDefault,
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
                if (audioStreamIndex == null && stream.isDefault) {
                    audioStreamIndex = stream.index
                }

                val track = Track(
                    id = stream.index,
                    label = stream.title,
                    language = stream.language,
                    codec = stream.codec,
                    selected = if (audioStreamIndex != null) stream.index == audioStreamIndex else false,
                    supported = true,
                    isExternal = stream.isExternal,
                    isForced = stream.isForced,
                    isHearingImpaired = stream.isHearingImpaired,
                )

                audio.add(track)

                Timber.d("Audio track: $stream")
            }
        }

        return Triple(castTracks.toList(), subtitles.toList(), audio.toList())
    }

    override fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        audioStreamIndex = null
        subtitleStreamIndex = null
        isSessionRestored = true
        isReporting = false
        itemCache.clear()

        scope.launch {
            val initialItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = BaseItemKind.fromName(itemKind),
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning
            )

            if (initialItem != null) {
                val client = remoteMediaClient ?: return@launch
                val result = buildMediaInfo(initialItem) ?: return@launch

                val castItem = CastMediaItem(
                    item = initialItem,
                    playbackInfo = result.playbackInfo,
                    subtitleTracks = result.subtitleTracks,
                    audioTracks = result.audioTracks
                )

                val loadRequest = MediaLoadRequestData.Builder()
                    .setMediaInfo(result.mediaInfo)
                    .setAutoplay(true)
                    .setCurrentTime(initialItem.playbackPosition)
                    .build()

                itemCache[initialItem.itemId] = castItem
                _currentItem.value = castItem

                client.load(loadRequest).setResultCallback { callbackResult ->
                    if (callbackResult.status.isSuccess) {
                        subtitleStreamIndex?.let {
                            client.setActiveMediaTracks(longArrayOf(it.toLong()))
                        }
                    } else {
                        Timber.e("Error: ${callbackResult.status.statusMessage}")
                        _currentItem.value = null
                    }
                }
            }
        }
    }

    private fun manageQueue(itemId: UUID) {
        scope.launch {
            val status = remoteMediaClient?.mediaStatus ?: return@launch
            val queueItems = status.queueItems

            playlistManager.setCurrentMediaItemIndex(itemId)

            val nextItem = playlistManager.getNextPlayerItem()
            val prevItem = playlistManager.getPreviousPlayerItem()

            val queuedItemIds = queueItems.mapNotNull {
                it.media?.customData?.optString("itemId")?.takeIf { id -> id.isNotEmpty() }
            }.toSet()

            nextItem?.let { next ->
                if (next.itemId.toString() !in queuedItemIds) {
                    queueNextItem(next)
                } else {
                    Timber.d("Item ${next.name} already in Cast queue.")
                }
            }

            prevItem?.let { prev ->
                if (prev.itemId.toString() !in queuedItemIds) {
                    queuePreviousItem(prev)
                } else {
                    Timber.d("Item ${prev.name} already in Cast queue.")
                }
            }

            _playerState.update {
                it.copy(
                    hasNextItem = nextItem != null,
                    hasPreviousItem = prevItem != null
                )
            }
        }
    }

    suspend fun queueNextItem(item: PlayerItem) {
        val result = buildMediaInfo(item) ?: return
        itemCache[item.itemId] = CastMediaItem(
            item = item,
            playbackInfo = result.playbackInfo,
            subtitleTracks = result.subtitleTracks,
            audioTracks = result.audioTracks
        )

        queueMutex.withLock {
            val client = remoteMediaClient ?: return@withLock
            val status = client.mediaStatus ?: return@withLock

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
        }
    }

    suspend fun queuePreviousItem(item: PlayerItem) {
        val result = buildMediaInfo(item) ?: return
        itemCache[item.itemId] = CastMediaItem(
            item = item,
            playbackInfo = result.playbackInfo,
            subtitleTracks = result.subtitleTracks,
            audioTracks = result.audioTracks
        )

        queueMutex.withLock {
            val client = remoteMediaClient ?: return@withLock
            val status = client.mediaStatus ?: return@withLock

            val queueItem = MediaQueueItem.Builder(result.mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(20.0)
                .build()

            val currentItemId = status.currentItemId

            client.queueInsertItems(arrayOf(queueItem), currentItemId, null)
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

        activeIds.remove(subtitleStreamIndex?.toLong())

        if (track != null) activeIds.add(track.id.toLong())
        subtitleStreamIndex = track?.id

        client.setActiveMediaTracks(activeIds.toLongArray()).setResultCallback { result ->
            if (result.status.isSuccess) {
                val activeTrackIds = client.mediaStatus?.activeTrackIds?.toList() ?: emptyList()

                _currentItem.update { item ->
                    val subtitleTracks = item?.subtitleTracks?.map {
                        it.copy(selected = activeTrackIds.contains(it.id.toLong()))
                    } ?: emptyList()

                    item?.copy(
                        subtitleTracks = subtitleTracks,
                    )
                }

                Timber.d("Selected subtitle track: $track")
            }
        }
    }

    override fun setAudioTrack(track: Track?, itemId: UUID?) {
        if (itemId == null || track == null) return
        if (audioStreamIndex == track.id) return
        audioStreamIndex = track.id

        // Update to avoid Ui glitches
        _currentItem.update { item ->
            val audioTracks = item?.audioTracks?.map {
                it.copy(selected = it.id == track.id)
            } ?: emptyList()

            item?.copy(
                audioTracks = audioTracks,
            )
        }

        scope.launch {
            val client = remoteMediaClient ?: return@launch
            val cachedMedia = itemCache[itemId] ?: return@launch
            val currentPositionMs = client.approximateStreamPosition

            // Request new playback info from Jellyfin with the selected audio track index
            val result = buildMediaInfo(cachedMedia.item) ?: return@launch

            // Update the current item
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
                        audioTracks = result.audioTracks
                    )

                    itemCache[itemId] = updatedMedia
                    _currentItem.value = updatedMedia

                    Timber.d("Selected audio track: $track")
                }
            }
        }
    }

    override fun stop() {
        remoteMediaClient?.stop()
        clearSession()
    }
}
