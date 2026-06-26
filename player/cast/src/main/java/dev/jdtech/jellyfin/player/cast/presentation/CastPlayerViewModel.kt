package dev.jdtech.jellyfin.player.cast.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.player.cast.CastPlayerController
import dev.jdtech.jellyfin.player.cast.CastSessionManager
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackState
import dev.jdtech.jellyfin.player.cast.models.Device
import dev.jdtech.jellyfin.player.core.R
import dev.jdtech.jellyfin.player.core.domain.PlaylistManager
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.PlayerMediaType
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import dev.jdtech.jellyfin.player.core.domain.utils.SegmentUtils
import dev.jdtech.jellyfin.player.core.domain.utils.SegmentUtils.getSkipButtonTextStringId
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.PlayMethod
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CastPlayerViewModel
@Inject constructor(
    val sessionManager: CastSessionManager,
    val playerController: CastPlayerController,
    private val playlistManager: PlaylistManager,
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    data class CurrentItemTitle(
        val seriesName: String? = null,
        val episodeInfo: String? = null,
        val title: String,
    )

    data class UiState(
        val connectionState: CastConnectionState = CastConnectionState.DISCONNECTED,
        val playbackState: CastPlaybackState = CastPlaybackState(),
        val availableDevices: List<Device> = emptyList(),
        val connectedDevice: Device? = null,
        val volume: Float = 1f,
        val currentItemTitle: CurrentItemTitle,
        val currentItemPosterUrl: String?,
        val isMovie: Boolean,
        val defaultAspectRatio: Float,
        val trickplayAspectRatio: Float?,
        val currentSegment: FindroidSegment?,
        val currentSkipButtonStringRes: Int,
        val currentTrickplay: Trickplay?,
        val currentChapters: List<PlayerChapter>,
        val fileLoaded: Boolean,
        val audioTracks: List<Track> = emptyList(),
        val subtitleTracks: List<Track> = emptyList(),
    )

    private val _internalUiState =
        MutableStateFlow(
            UiState(
                connectionState = sessionManager.connectionState.value,
                availableDevices = sessionManager.availableDevices.value,
                connectedDevice = sessionManager.connectedDevice.value,
                playbackState = playerController.playbackState.value,
                volume = playerController.volume.value,

                currentItemTitle = CurrentItemTitle(title = ""),
                currentItemPosterUrl = null,
                isMovie = false,
                defaultAspectRatio = 16f / 10f,
                trickplayAspectRatio = null,
                currentSegment = null,
                currentSkipButtonStringRes = R.string.player_controls_skip_intro,
                currentTrickplay = null,
                currentChapters = emptyList(),
                fileLoaded = false,
                audioTracks = emptyList(),
                subtitleTracks = emptyList()
            )
        )

    val uiState: StateFlow<UiState> = _internalUiState.asStateFlow()


    private var currentMediaItemSegments: List<FindroidSegment> = emptyList()
    var currentItemId: UUID? = null
    var hasPreviousMediaItem = false
    var hasNextMediaItem = false

    init {
        sessionManager.connectionState
            .onEach { value -> _internalUiState.update { it.copy(connectionState = value) } }
            .launchIn(viewModelScope)

        sessionManager.availableDevices
            .onEach { value -> _internalUiState.update { it.copy(availableDevices = value) } }
            .launchIn(viewModelScope)

        sessionManager.connectedDevice
            .onEach { value -> _internalUiState.update { it.copy(connectedDevice = value) } }
            .launchIn(viewModelScope)

        playerController.playbackState
            .onEach { value -> _internalUiState.update { it.copy(playbackState = value) } }
            .launchIn(viewModelScope)

        playerController.volume
            .onEach { value -> _internalUiState.update { it.copy(volume = value) } }
            .launchIn(viewModelScope)

        playerController.subtitleTracks
            .onEach { value -> _internalUiState.update { it.copy(subtitleTracks = value) } }
            .launchIn(viewModelScope)

        playerController.audioTracks
            .onEach { value -> _internalUiState.update { it.copy(audioTracks = value) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            playerController.currentItem.collect { item ->
                if (item != null) {
                    onMediaItemTransition(item)
                } else {
                    onMediaItemCleared()
                }

                Timber.d("CurrentItem: $item")
            }
        }

        viewModelScope.launch {
            playerController.restoringItemId.collect { itemIdStr ->
                if (itemIdStr != null) {
                    try {
                        val itemId = UUID.fromString(itemIdStr)
                        val findroidItem = repository.getItem(itemId)
                        if (findroidItem != null) {
                            val itemKind = when (findroidItem) {
                                is FindroidMovie -> BaseItemKind.MOVIE
                                is FindroidEpisode -> BaseItemKind.EPISODE
                                else -> return@collect
                            }
                            val playerItem = playlistManager.getInitialItem(
                                itemId = itemId,
                                itemKind = itemKind,
                                mediaSourceIndex = null,
                                startFromBeginning = false
                            )
                            if (playerItem != null) {
                                playerController.restoreItem(playerItem)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }

        viewModelScope.launch {
            playerController.subtitleTracks.collect { tracks ->
                _internalUiState.update { it.copy(subtitleTracks = tracks) }
            }
        }

        if (
            appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton) ||
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        ) {
            viewModelScope.launch {
                while (true) {
                    playerController.playbackState.collect { state ->
                        updateCurrentSegment(state.currentPosition)
                    }
                    delay(1000L.milliseconds)
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                updatePlaybackProgress()
                delay(5000L.milliseconds)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            reportPlaybackStop()
        }
    }

    /**
     * Initializes playback of a specific Jellyfin item on the connected cast device.
     * Fetches the initial payload from the repository and loads it via [CastPlayerController].
     */
    fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        viewModelScope.launch {
            val initialItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = BaseItemKind.fromName(itemKind),
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning
            )
            currentItemId = itemId
            _internalUiState.update { it.copy(fileLoaded = false) }
            if (initialItem != null) {
                playerController.loadItem(
                    initialItem,
                    if (startFromBeginning) 0L else initialItem.playbackPosition
                )
            }
        }
    }

    /**
     * Handles the transition when a new media item starts playing.
     * Updates the UI state with metadata (title, poster, aspect ratio, chapters),
     * notifies the Jellyfin server that playback started, and fetches supplementary
     * data like skip segments, trickplay thumbnails, and audio tracks.
     */
    private suspend fun onMediaItemTransition(item: PlayerItem) {
        Timber.d("Cast MediaItem transition: ${item.itemId}")
        currentItemId = item.itemId
        val isMovie = item.mediaType == PlayerMediaType.MOVIE

        val defaultRatio = when (item.mediaType) {
            PlayerMediaType.EPISODE -> 16f / 9f
            PlayerMediaType.MOVIE -> 2f / 3f
            else -> 16f / 10f
        }

        val trickplayRatio = item.trickplayInfo?.let {
            if (it.width > 0 && it.height > 0) it.width.toFloat() / it.height.toFloat() else null
        }

        val itemTitle = if (item.parentIndexNumber != null && item.indexNumber != null) {
            val parentIndex = item.parentIndexNumber.toString().padStart(2, '0')
            val index = item.indexNumber.toString().padStart(2, '0')
            val episodeInfo = if (item.indexNumberEnd == null) {
                "S$parentIndex - E$index"
            } else {
                val indexEnd = item.indexNumberEnd.toString().padStart(2, '0')
                "S$parentIndex - E$index:$indexEnd"
            }

            CurrentItemTitle(
                seriesName = item.seriesName,
                episodeInfo = episodeInfo,
                title = item.name
            )
        } else {
            CurrentItemTitle(title = item.name)
        }

        _internalUiState.update {
            it.copy(
                currentItemTitle = itemTitle,
                currentItemPosterUrl = item.posterUrl,
                isMovie = isMovie,
                defaultAspectRatio = defaultRatio,
                trickplayAspectRatio = trickplayRatio,
                currentSegment = null,
                currentChapters = item.chapters,
                fileLoaded = true,
            )
        }

        val playbackInfo = playerController.playbackInfoResponse.value
        val mediaSource = playbackInfo?.mediaSources?.firstOrNull()
        val playMethod = when {
            mediaSource?.supportsDirectPlay ?: false -> PlayMethod.DIRECT_PLAY
            mediaSource?.supportsDirectStream ?: false -> PlayMethod.DIRECT_STREAM
            else -> PlayMethod.TRANSCODE
        }

        repository.postPlaybackStart(
            item.itemId,
            playMethod,
            mediaSource?.id,
            playbackInfo?.playSessionId
        )

        if (appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton) ||
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        ) {
            getSegments(item.itemId)
        }

        if (appPreferences.getValue(appPreferences.playerTrickplay)) {
            getTrickplay(item)
        }

        playlistManager.setCurrentMediaItemIndex(item.itemId)

        val previousItem = playlistManager.getPreviousPlayerItem()
        if (previousItem != null) {
            playerController.queuePreviousItem(previousItem)
        }

        val nextItem = playlistManager.getNextPlayerItem()
        if (nextItem != null) {
            playerController.queueNextItem(nextItem)
        }

        hasPreviousMediaItem = playlistManager.getPreviousPlayerItem() != null
        hasNextMediaItem = playlistManager.getNextPlayerItem() != null
    }

    /**
     * Periodically reports the current playback progress to the Jellyfin server
     * so that watch states and resume points are saved correctly.
     */
    private suspend fun updatePlaybackProgress() {
        val item = playerController.currentItem.value ?: return
        val state = playerController.playbackState.value
        val playbackInfo = playerController.playbackInfoResponse.value
        val mediaSource = playbackInfo?.mediaSources?.firstOrNull()
        val playMethod = when {
            mediaSource?.supportsDirectPlay ?: false -> PlayMethod.DIRECT_PLAY
            mediaSource?.supportsDirectStream ?: false -> PlayMethod.DIRECT_STREAM
            else -> PlayMethod.TRANSCODE
        }

        if (state.duration > 0) {
            try {
                repository.postPlaybackProgress(
                    item.itemId,
                    state.currentPosition.times(10000),
                    !state.isPlaying,
                    playMethod,
                    mediaSource?.id,
                    playbackInfo?.playSessionId
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    /**
     * Checks if the current playback position falls within a known "segment" (like intros or credits).
     * If auto-skip is enabled, it automatically skips the segment. Otherwise, it updates
     * the UI state to show a "Skip" button if the segment type matches the user's preferences.
     */
    private fun updateCurrentSegment(positionMs: Long) {
        if (currentMediaItemSegments.isEmpty()) return

        val currentSegment = currentMediaItemSegments.find { segment ->
            positionMs in segment.startTicks..<(segment.endTicks - 100L)
        }

        if (currentSegment == null) {
            if (_internalUiState.value.currentSegment != null) {
                _internalUiState.update { it.copy(currentSegment = null) }
            }
            return
        }

        val segmentsAutoSkip = appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        val segmentsAutoSkipTypes =
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkipType)
        val segmentsAutoSkipMode =
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkipMode)
        val segmentsSkipButtonTypes =
            appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButtonType)

        if (segmentsAutoSkip &&
            segmentsAutoSkipTypes.contains(currentSegment.type.toString()) &&
            segmentsAutoSkipMode == Constants.PlayerMediaSegmentsAutoSkip.ALWAYS
        ) {
            skipSegment(currentSegment)
        } else if (segmentsSkipButtonTypes.contains(currentSegment.type.toString())) {
            _internalUiState.update {
                it.copy(
                    currentSegment = currentSegment,
                    currentSkipButtonStringRes = getSkipButtonTextStringId(
                        currentSegment,
                        shouldSkipToNextEpisode(currentSegment)
                    ),
                )
            }
        } else {
            _internalUiState.update { it.copy(currentSegment = null) }
        }

        Timber.d("Updated current segment: ${uiState.value.currentSegment?.type}")
    }

    /**
     * Executes the skip action for a given segment. If the segment is the end credits
     * and the threshold allows it, it skips directly to the next episode. Otherwise,
     * it seeks past the segment's end boundary.
     */
    fun skipSegment(segment: FindroidSegment) {
        if (shouldSkipToNextEpisode(segment)) {
            playNextItem()
        } else {
            playerController.seekTo(segment.endTicks)
        }
        _internalUiState.update { it.copy(currentSegment = null) }
    }

    /**
     * Skips playback to the next item in the playlist queue.
     */
    fun playNextItem() {
        _internalUiState.update { it.copy(fileLoaded = false) }
        playerController.seekToNext()
    }

    /**
     * Reverts playback to the previous item in the playlist queue.
     */
    fun playPreviousItem() {
        _internalUiState.update { it.copy(fileLoaded = false) }
        playerController.seekToPrevious()
    }

    /**
     * Handles the selection of a new audio track from the UI.
     * Updates the local UI state and tells the [playerController] to switch the track.
     */
    fun onAudioTrackSelected(track: Track?) {
        _internalUiState.update { state ->
            state.copy(audioTracks = state.audioTracks.map { it.copy(selected = it.id == track?.id) })
        }
        playerController.setAudioTrack(track, currentItemId)
    }

    /**
     * Handles the selection of a new subtitle track from the UI.
     * Updates the local UI state and tells the [playerController] to switch the track.
     */
    fun onSubtitleTrackSelected(track: Track?) {
        _internalUiState.update { state ->
            state.copy(subtitleTracks = state.subtitleTracks.map { it.copy(selected = it.id == track?.id) })
        }
        playerController.setSubtitleTrack(track)
    }

    /**
     * Resets the entire UI state and flushes cache when the remote receiver stops
     * playing media or disconnects.
     */
    private fun onMediaItemCleared() {
        currentMediaItemSegments = emptyList()
        hasPreviousMediaItem = false
        hasNextMediaItem = false
        _internalUiState.update {
            it.copy(
                currentItemTitle = CurrentItemTitle(title = ""),
                currentItemPosterUrl = null,
                isMovie = false,
                defaultAspectRatio = 16f / 10f,
                trickplayAspectRatio = null,
                currentSegment = null,
                currentSkipButtonStringRes = R.string.player_controls_skip_intro,
                currentTrickplay = null,
                currentChapters = emptyList(),
                fileLoaded = false,
                audioTracks = emptyList(),
                subtitleTracks = emptyList(),
            )
        }
    }

    /**
     * Determines whether skipping a specific segment (usually an outro or credits)
     * should automatically jump to the next episode instead of just seeking ahead,
     * based on the user's threshold preferences.
     */
    private fun shouldSkipToNextEpisode(segment: FindroidSegment): Boolean {
        return SegmentUtils.shouldSkipToNextEpisode(
            segment = segment,
            hasNextMediaItem = hasNextMediaItem,
            playerDurationMillis = playerController.playbackState.value.duration,
            nextEpisodeThreshold = appPreferences.getValue(appPreferences.playerMediaSegmentsNextEpisodeThreshold)
        )
    }

    /**
     * Fetches special media segments (intros, credits) for the given item from Jellyfin.
     */
    private suspend fun getSegments(itemId: UUID) {
        try {
            currentMediaItemSegments = repository.getSegments(itemId)
        } catch (e: Exception) {
            currentMediaItemSegments = emptyList()
            Timber.e(e)
        }
    }

    /**
     * Downloads trickplay (BIF/Thumbnail) data for scrubbing previews, slices the sprite sheet
     * into individual bitmaps, and updates the UI state so the seek bar can show thumbnails.
     */
    private suspend fun getTrickplay(item: PlayerItem) {
        val trickplayInfo = item.trickplayInfo ?: return
        withContext(Dispatchers.Default) {
            try {
                val maxIndex = ceil(
                    trickplayInfo.thumbnailCount.toDouble()
                        .div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)
                ).toInt()
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0..maxIndex) {
                    repository.getTrickplayData(item.itemId, trickplayInfo.width, i)
                        ?.let { byteArray ->
                            val fullBitmap =
                                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            for (offsetY in 0..<trickplayInfo.height * trickplayInfo.tileHeight step trickplayInfo.height) {
                                for (offsetX in 0..<trickplayInfo.width * trickplayInfo.tileWidth step trickplayInfo.width) {
                                    if (bitmaps.size < trickplayInfo.thumbnailCount) {
                                        val bitmap = Bitmap.createBitmap(
                                            fullBitmap,
                                            offsetX,
                                            offsetY,
                                            trickplayInfo.width,
                                            trickplayInfo.height
                                        )
                                        bitmaps.add(bitmap)
                                    }
                                }
                            }
                        }
                }
                _internalUiState.update {
                    it.copy(currentTrickplay = Trickplay(trickplayInfo.interval, bitmaps))
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private suspend fun reportPlaybackStop() {
        val itemId = currentItemId ?: return
        val state = playerController.playbackState.value
        val playbackInfo = playerController.playbackInfoResponse.value

        if (state.duration > 0) {
            val positionTicks = state.currentPosition.times(10000)
            val playedPercentage =
                (state.currentPosition.toFloat() / state.duration.toFloat() * 100).toInt()

            try {
                repository.postPlaybackStop(
                    itemId = itemId,
                    positionTicks = positionTicks,
                    playedPercentage = playedPercentage,
                    mediaSourceId = playbackInfo?.mediaSources?.firstOrNull()?.id,
                    playSessionId = playbackInfo?.playSessionId
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to post playback stop")
            }
        }
    }
}