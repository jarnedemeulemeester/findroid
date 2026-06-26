package dev.jdtech.jellyfin.player.cast

import android.content.Context
import androidx.core.net.toUri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.player.cast.devices.Chromecast
import dev.jdtech.jellyfin.player.cast.devices.ChromecastH265
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackState
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
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackInfoResponse
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class CastPlayerControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi
) : CastPlayerController {

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    override val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    private val _restoringItemId = MutableStateFlow<String?>(null)
    override val restoringItemId: StateFlow<String?> = _restoringItemId.asStateFlow()

    private val _playbackState = MutableStateFlow(CastPlaybackState())
    override val playbackState: StateFlow<CastPlaybackState> = _playbackState.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
    override val subtitleTracks: StateFlow<List<Track>> = _subtitleTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
    override val audioTracks: StateFlow<List<Track>> = _audioTracks.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _playbackInfoResponse = MutableStateFlow<PlaybackInfoResponse?>(null)
    override val playbackInfoResponse: StateFlow<PlaybackInfoResponse?> =
        _playbackInfoResponse.asStateFlow()

    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }
    private var remoteMediaClient: RemoteMediaClient? = null
    private var castSession: CastSession? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private val itemCache = mutableMapOf<String, PlayerItem>()

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updatePlaybackState()
        }

        override fun onMetadataUpdated() {
            updatePlaybackState()
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            setupSession(session)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            setupSession(session)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            clearSession()
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            clearSession()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }

    init {
        castContext.sessionManager.addSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
        castContext.sessionManager.currentCastSession?.let {
            setupSession(it)
        }
    }

    private fun setupSession(session: CastSession) {
        castSession = session
        remoteMediaClient = session.remoteMediaClient
        remoteMediaClient?.registerCallback(remoteMediaClientCallback)
        startProgressUpdate()
    }

    private fun clearSession() {
        progressJob?.cancel()
        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        castSession = null
        remoteMediaClient = null
        _currentItem.value = null
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

    private suspend fun getPlaybackInfo(
        item: PlayerItem,
        audioStreamIndex: Int?
    ): PlaybackInfoResponse? {
        val userId = jellyfinApi.userId
        val profile = if (true /* logic for chromecast 4k */) {
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
    ): MediaInfo? {
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
        _playbackInfoResponse.value = playbackInfo

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

        _subtitleTracks.value = subtitles
        _audioTracks.value = audio

        if (castTracks.isNotEmpty()) {
            mediaInfoBuilder.setMediaTracks(castTracks)
        }

        return mediaInfoBuilder.build()
    }

    override fun loadItem(item: PlayerItem, startPosition: Long) {
        scope.launch {
            itemCache.clear()
            itemCache[item.itemId.toString()] = item

            _currentItem.value = item
            _restoringItemId.value = null
            val client = remoteMediaClient ?: return@launch

            val mediaInfoBuilder = buildMediaInfo(item) ?: return@launch

            val loadRequest = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfoBuilder)
                .setAutoplay(true)
                .setCurrentTime(startPosition)
                .build()

            client.load(loadRequest)
        }
    }

    override fun restoreItem(item: PlayerItem) {
        itemCache[item.itemId.toString()] = item
        _currentItem.value = item
        _restoringItemId.value = null
    }

    override fun queueNextItem(item: PlayerItem) {
        scope.launch {
            itemCache[item.itemId.toString()] = item
            val client = remoteMediaClient ?: return@launch

            val mediaInfo = buildMediaInfo(item) ?: return@launch
            val status = client.mediaStatus
            if (status?.queueItems?.lastOrNull()?.media?.contentId == mediaInfo.contentId) return@launch

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

            client.queueInsertItems(arrayOf(queueItem), nextItemId, null)
        }
    }

    override fun queuePreviousItem(item: PlayerItem) {
        scope.launch {
            itemCache[item.itemId.toString()] = item
            val client = remoteMediaClient ?: return@launch

            val mediaInfo = buildMediaInfo(item) ?: return@launch
            val status = client.mediaStatus
            if (status?.queueItems?.firstOrNull()?.media?.contentId == mediaInfo.contentId) return@launch

            val queueItem = MediaQueueItem.Builder(mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(20.0)
                .build()

            val currentItemId = client.mediaStatus?.currentItemId ?: MediaQueueItem.INVALID_ITEM_ID
            client.queueInsertItems(arrayOf(queueItem), currentItemId, null)
        }
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
        client.setActiveMediaTracks(activeIds.toLongArray()).setResultCallback { result ->
            if (result.status.isSuccess) {
                val activeTrackIds = client.mediaStatus?.activeTrackIds?.toList() ?: emptyList()

                _subtitleTracks.value = _subtitleTracks.value.map {
                    it.copy(selected = activeTrackIds.contains(it.id.toLong()))
                }

                Timber.d("Selected subtitle track: $track")
            }
        }
    }

    override fun setAudioTrack(track: Track?, itemId: UUID?) {
        if (itemId == null || track == null) return
        scope.launch {
            val client = remoteMediaClient ?: return@launch
            val item = itemCache[itemId.toString()] ?: return@launch

            val currentQueueItem = client.currentItem ?: return@launch
            val currentPositionMs = client.approximateStreamPosition

            // Request new playback info from Jellyfin with the selected audio track index
            val newMediaInfo = buildMediaInfo(item, track.id) ?: return@launch

            // Update the current item in the Cast queue with the new MediaInfo (and stream URL)
            val updatedQueueItem = MediaQueueItem.Builder(newMediaInfo)
                .setStartTime(currentPositionMs / 1000.0)
                .setAutoplay(true)
                .apply {
                    currentQueueItem.customData?.let { setCustomData(it) }
                }
                .build()

            client.queueInsertAndPlayItem(updatedQueueItem, currentQueueItem.itemId, null)
                .setResultCallback { result ->
                    if (result.status.isSuccess) {
                        if (_audioTracks.value.any { it.id == track.id }) {
                            _audioTracks.value =
                                _audioTracks.value.map { it.copy(selected = it.id == track.id) }
                        }

                        client.queueRemoveItems(intArrayOf(currentQueueItem.itemId), null)

                        Timber.d("Selected audio track: $track")
                    }
                }
        }
    }

    override fun stop() {
        remoteMediaClient?.stop()
    }
}
