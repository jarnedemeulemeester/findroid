package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidSegmentType
import dev.jdtech.jellyfin.models.PlayerChapter
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.PlayerSegment
import dev.jdtech.jellyfin.models.Trickplay
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.player.video.R
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class PlayerActivityViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), Player.Listener {
    val player: Player

    private val _uiState = MutableStateFlow(
        UiState(
            currentItemTitle = "",
            currentSegment = null,
            currentTrickplay = null,
            currentChapters = null,
            fileLoaded = false,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<PlayerEvents>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    data class UiState(
        val currentItemTitle: String,
        val currentSegment: PlayerSegment?,
        val currentTrickplay: Trickplay?,
        val currentChapters: List<PlayerChapter>?,
        val fileLoaded: Boolean,
    )

    private var items: Array<PlayerItem> = arrayOf()

    private val trackSelector = DefaultTrackSelector(application)
    var playWhenReady = true
    private var currentMediaItemIndex = savedStateHandle["mediaItemIndex"] ?: 0
    private var playbackPosition: Long = savedStateHandle["position"] ?: 0
    private var currentMediaItemSegments: List<PlayerSegment>? = null

    var playbackSpeed: Float = 1f

    private val handler = Handler(Looper.getMainLooper())

    init {
        if (appPreferences.getValue(appPreferences.playerMpv)) {
            val trackSelectionParameters = TrackSelectionParameters.Builder()
                .setPreferredAudioLanguage(appPreferences.getValue(appPreferences.preferredAudioLanguage))
                .setPreferredTextLanguage(appPreferences.getValue(appPreferences.preferredSubtitleLanguage))
                .build()
            player = MPVPlayer(
                context = application,
                requestAudioFocus = true,
                trackSelectionParameters = trackSelectionParameters,
                seekBackIncrement = appPreferences.getValue(appPreferences.playerSeekBackInc),
                seekForwardIncrement = appPreferences.getValue(appPreferences.playerSeekForwardInc),
                videoOutput = appPreferences.getValue(appPreferences.playerMpvVo),
                audioOutput = appPreferences.getValue(appPreferences.playerMpvAo),
                hwDec = appPreferences.getValue(appPreferences.playerMpvHwdec),
                pauseAtEndOfMediaItems = true,
            )
        } else {
            val renderersFactory =
                DefaultRenderersFactory(application).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
                )
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTunnelingEnabled(true)
                    .setPreferredAudioLanguage(appPreferences.getValue(appPreferences.preferredAudioLanguage))
                    .setPreferredTextLanguage(appPreferences.getValue(appPreferences.preferredSubtitleLanguage)),
            )
            player = ExoPlayer.Builder(application, renderersFactory)
                .setTrackSelector(trackSelector)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */
                    true,
                )
                .setSeekBackIncrementMs(appPreferences.getValue(appPreferences.playerSeekBackInc))
                .setSeekForwardIncrementMs(appPreferences.getValue(appPreferences.playerSeekForwardInc))
                .setPauseAtEndOfMediaItems(true)
                .build()
        }
    }

    fun initializePlayer(
        items: Array<PlayerItem>,
    ) {
        this.items = items
        player.addListener(this)

        viewModelScope.launch {
            val mediaItems = mutableListOf<MediaItem>()
            try {
                for (item in items) {
                    val streamUrl = item.mediaSourceUri
                    val mediaSubtitles = item.externalSubtitles.map { externalSubtitle ->
                        MediaItem.SubtitleConfiguration.Builder(externalSubtitle.uri)
                            .setLabel(externalSubtitle.title.ifBlank { application.getString(R.string.external) })
                            .setMimeType(externalSubtitle.mimeType)
                            .setLanguage(externalSubtitle.language)
                            .build()
                    }

                    Timber.d("Stream url: $streamUrl")
                    val mediaItem =
                        MediaItem.Builder()
                            .setMediaId(item.itemId.toString())
                            .setUri(streamUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(item.name)
                                    .build(),
                            )
                            .setSubtitleConfigurations(mediaSubtitles)
                            .build()
                    mediaItems.add(mediaItem)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            val startPosition = if (playbackPosition == 0L) {
                items.getOrNull(currentMediaItemIndex)?.playbackPosition ?: C.TIME_UNSET
            } else {
                playbackPosition
            }

            player.setMediaItems(
                mediaItems,
                currentMediaItemIndex,
                startPosition,
            )
            player.prepare()
            player.play()
            pollPosition(player)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun releasePlayer() {
        val mediaId = player.currentMediaItem?.mediaId
        val position = player.currentPosition
        val duration = player.duration
        GlobalScope.launch {
            delay(1000L)
            try {
                jellyfinRepository.postPlaybackStop(
                    UUID.fromString(mediaId),
                    position.times(10000),
                    position.div(duration.toFloat()).times(100).toInt(),
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        _uiState.update { it.copy(currentTrickplay = null) }
        playWhenReady = false
        playbackPosition = 0L
        currentMediaItemIndex = 0
        player.removeListener(this)
        player.release()
    }

    private fun pollPosition(player: Player) {
        val playbackProgressRunnable = object : Runnable {
            override fun run() {
                savedStateHandle["position"] = player.currentPosition
                viewModelScope.launch {
                    if (player.currentMediaItem != null && player.currentMediaItem!!.mediaId.isNotEmpty()) {
                        val itemId = UUID.fromString(player.currentMediaItem!!.mediaId)
                        try {
                            jellyfinRepository.postPlaybackProgress(
                                itemId,
                                player.currentPosition.times(10000),
                                !player.isPlaying,
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                handler.postDelayed(this, 5000L)
            }
        }
        val segmentCheckRunnable = object : Runnable {
            override fun run() {
                updateCurrentSegment()
                handler.postDelayed(this, 1000L)
            }
        }
        if (appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton) ||
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        ) {
            handler.post(segmentCheckRunnable)
        }
        handler.post(playbackProgressRunnable)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.d("Playing MediaItem: ${mediaItem?.mediaId}")
        savedStateHandle["mediaItemIndex"] = player.currentMediaItemIndex
        viewModelScope.launch {
            try {
                items.first { it.itemId.toString() == player.currentMediaItem?.mediaId }
                    .let { item ->
                        val itemTitle = if (item.parentIndexNumber != null && item.indexNumber != null) {
                            if (item.indexNumberEnd == null) {
                                "S${item.parentIndexNumber}:E${item.indexNumber} - ${item.name}"
                            } else {
                                "S${item.parentIndexNumber}:E${item.indexNumber}-${item.indexNumberEnd} - ${item.name}"
                            }
                        } else {
                            item.name
                        }
                        _uiState.update {
                            it.copy(
                                currentItemTitle = itemTitle,
                                currentSegment = null,
                                currentChapters = item.chapters,
                                fileLoaded = false,
                            )
                        }

                        currentMediaItemSegments = item.segments

                        jellyfinRepository.postPlaybackStart(item.itemId)

                        if (appPreferences.getValue(appPreferences.playerTrickplay)) {
                            getTrickplay(item)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // Report playback stopped for current item and transition to the next one
        if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && player.playbackState == ExoPlayer.STATE_READY) {
            viewModelScope.launch {
                val mediaId = player.currentMediaItem?.mediaId
                val position = player.currentPosition
                val duration = player.duration
                try {
                    jellyfinRepository.postPlaybackStop(
                        UUID.fromString(mediaId),
                        position.times(10000),
                        position.div(duration.toFloat()).times(100).toInt(),
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                }
                player.seekToNextMediaItem()
                player.play()
            }
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        var stateString = "UNKNOWN_STATE             -"
        when (state) {
            ExoPlayer.STATE_IDLE -> {
                stateString = "ExoPlayer.STATE_IDLE      -"
            }
            ExoPlayer.STATE_BUFFERING -> {
                stateString = "ExoPlayer.STATE_BUFFERING -"
            }
            ExoPlayer.STATE_READY -> {
                stateString = "ExoPlayer.STATE_READY     -"
                _uiState.update { it.copy(fileLoaded = true) }
            }
            ExoPlayer.STATE_ENDED -> {
                stateString = "ExoPlayer.STATE_ENDED     -"
                eventsChannel.trySend(PlayerEvents.NavigateBack)
            }
        }
        Timber.d("Changed player state to $stateString")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing Player ViewModel")
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
    }

    fun switchToTrack(trackType: @C.TrackType Int, index: Int) {
        // Index -1 equals disable track
        if (index == -1) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(trackType)
                .setTrackTypeDisabled(trackType, true)
                .build()
        } else {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(player.currentTracks.groups.filter { it.type == trackType && it.isSupported }[index].mediaTrackGroup, 0),
                )
                .setTrackTypeDisabled(trackType, false)
                .build()
        }
    }

    fun selectSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }

    private suspend fun getTrickplay(item: PlayerItem) {
        val trickplayInfo = item.trickplayInfo ?: return
        Timber.d("Trickplay Resolution: ${trickplayInfo.width}")

        withContext(Dispatchers.Default) {
            val maxIndex = ceil(trickplayInfo.thumbnailCount.toDouble().div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)).toInt()
            val bitmaps = mutableListOf<Bitmap>()

            for (i in 0..maxIndex) {
                jellyfinRepository.getTrickplayData(
                    item.itemId,
                    trickplayInfo.width,
                    i,
                )?.let { byteArray ->
                    val fullBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    for (offsetY in 0..<trickplayInfo.height * trickplayInfo.tileHeight step trickplayInfo.height) {
                        for (offsetX in 0..<trickplayInfo.width * trickplayInfo.tileWidth step trickplayInfo.width) {
                            val bitmap = Bitmap.createBitmap(fullBitmap, offsetX, offsetY, trickplayInfo.width, trickplayInfo.height)
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
            _uiState.update { it.copy(currentTrickplay = Trickplay(trickplayInfo.interval, bitmaps)) }
        }
    }

    private fun updateCurrentSegment() {
        if (currentMediaItemSegments.isNullOrEmpty()) {
            return
        }
        val milliSeconds = player.currentPosition

        val currentSegment = currentMediaItemSegments?.find { segment -> milliSeconds in segment.startTicks..<segment.endTicks }
        Timber.tag("SegmentInfo").d("currentSegment: %s", currentSegment)
        _uiState.update { it.copy(currentSegment = currentSegment) }
    }

    fun skipSegment(segment: PlayerSegment) {
        if (skipToNextEpisode(segment)) {
            player.seekToNextMediaItem()
        } else {
            player.seekTo((segment.endTicks))
        }
    }

    // Check if the outro segment's end time is within n milliseconds of the player's total duration
    private fun skipToNextEpisode(segment: PlayerSegment): Boolean {
        return if (segment.type == FindroidSegmentType.OUTRO && player.hasNextMediaItem()) {
            val segmentEndTimeMillis = segment.endTicks
            val playerDurationMillis = player.duration
            val thresholdMillis = playerDurationMillis - appPreferences.getValue(appPreferences.playerMediaSegmentsNextEpisodeThreshold)

            segmentEndTimeMillis > thresholdMillis
        } else {
            false
        }
    }

    fun getSkipButtonTextStringId(segment: PlayerSegment): Int {
        return when (skipToNextEpisode(segment)) {
            true -> R.string.player_controls_next_episode
            false -> when (segment.type) {
                FindroidSegmentType.INTRO -> R.string.player_controls_skip_intro
                FindroidSegmentType.OUTRO -> R.string.player_controls_skip_outro
                FindroidSegmentType.RECAP -> R.string.player_controls_skip_recap
                FindroidSegmentType.COMMERCIAL -> R.string.player_controls_skip_commercial
                FindroidSegmentType.PREVIEW -> R.string.player_controls_skip_preview
                else -> R.string.player_controls_skip_unknown
            }
        }
    }

    /**
     * Get chapters of current item
     * @return list of [PlayerChapter]
     */
    private fun getChapters(): List<PlayerChapter>? {
        return uiState.value.currentChapters
    }

    /**
     * Get the index of the current chapter
     * @return the index of the current chapter
     */
    private fun getCurrentChapterIndex(): Int? {
        val chapters = getChapters() ?: return null

        for (i in chapters.indices.reversed()) {
            if (chapters[i].startPosition < player.currentPosition) {
                return i
            }
        }

        return null
    }

    /**
     * Get the index of the next chapter
     * @return the index of the next chapter
     */
    private fun getNextChapterIndex(): Int? {
        val chapters = getChapters() ?: return null
        val currentChapterIndex = getCurrentChapterIndex() ?: return null

        return minOf(chapters.size - 1, currentChapterIndex + 1)
    }

    /**
     * Get the index of the previous chapter.
     * Only use this for seeking as it will return the current chapter when player position is more than 5 seconds past the start of the chapter
     * @return the index of the previous chapter
     */
    private fun getPreviousChapterIndex(): Int? {
        val chapters = getChapters() ?: return null
        val currentChapterIndex = getCurrentChapterIndex() ?: return null

        // Return current chapter when more than 5 seconds past chapter start
        if (player.currentPosition > chapters[currentChapterIndex].startPosition + 5000L) {
            return currentChapterIndex
        }

        return maxOf(0, currentChapterIndex - 1)
    }

    fun isFirstChapter(): Boolean? = getChapters()?.let { getCurrentChapterIndex() == 0 }
    fun isLastChapter(): Boolean? = getChapters()?.let { chapters -> getCurrentChapterIndex() == chapters.size - 1 }

    /**
     * Seek to chapter
     * @param [chapterIndex] the index of the chapter to seek to
     * @return the [PlayerChapter] which has been sought to
     */
    private fun seekToChapter(chapterIndex: Int): PlayerChapter? {
        return getChapters()?.getOrNull(chapterIndex)?.also { chapter ->
            player.seekTo(chapter.startPosition)
        }
    }

    /**
     * Seek to the next chapter
     * @return the [PlayerChapter] which has been sought to
     */
    fun seekToNextChapter(): PlayerChapter? {
        return getNextChapterIndex()?.let { seekToChapter(it) }
    }

    /**
     * Seek to the previous chapter
     * Will seek to start of current chapter if player position is more than 5 seconds past start of chapter
     * @return the [PlayerChapter] which has been sought to
     */
    fun seekToPreviousChapter(): PlayerChapter? {
        return getPreviousChapterIndex()?.let { seekToChapter(it) }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        eventsChannel.trySend(PlayerEvents.IsPlayingChanged(isPlaying))
    }
}

sealed interface PlayerEvents {
    data object NavigateBack : PlayerEvents
    data class IsPlayingChanged(val isPlaying: Boolean) : PlayerEvents
}
