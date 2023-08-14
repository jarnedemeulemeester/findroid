package dev.jdtech.jellyfin.viewmodels

import android.app.Application
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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.models.Intro
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.player.video.R
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.bif.BifData
import dev.jdtech.jellyfin.utils.bif.BifUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

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
            currentIntro = null,
            currentTrickPlay = null,
            fileLoaded = false,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack = _navigateBack.asSharedFlow()

    private val intros: MutableMap<UUID, Intro> = mutableMapOf()

    private val trickPlays: MutableMap<UUID, BifData> = mutableMapOf()

    var currentAudioTracks: MutableList<MPVPlayer.Companion.Track> = mutableListOf()
    var currentSubtitleTracks: MutableList<MPVPlayer.Companion.Track> = mutableListOf()

    data class UiState(
        val currentItemTitle: String,
        val currentIntro: Intro?,
        val currentTrickPlay: BifData?,
        val fileLoaded: Boolean,
    )

    private var items: Array<PlayerItem> = arrayOf()

    val trackSelector = DefaultTrackSelector(application)
    var playWhenReady = true
    private var currentMediaItemIndex = savedStateHandle["mediaItemIndex"] ?: 0
    private var playbackPosition: Long = savedStateHandle["position"] ?: 0

    var playbackSpeed: Float = 1f
    var disableSubtitle: Boolean = false

    private val handler = Handler(Looper.getMainLooper())

    init {
        if (appPreferences.playerMpv) {
            player = MPVPlayer(
                application,
                false,
                appPreferences,
            )
        } else {
            val renderersFactory =
                DefaultRenderersFactory(application).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
                )
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTunnelingEnabled(true)
                    .setPreferredAudioLanguage(appPreferences.preferredAudioLanguage)
                    .setPreferredTextLanguage(appPreferences.preferredSubtitleLanguage),
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
                .setSeekBackIncrementMs(appPreferences.playerSeekBackIncrement)
                .setSeekForwardIncrementMs(appPreferences.playerSeekForwardIncrement)
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

                    if (appPreferences.playerIntroSkipper) {
                        jellyfinRepository.getIntroTimestamps(item.itemId)?.let { intro ->
                            intros[item.itemId] = intro
                        }
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
            if (appPreferences.playerMpv) { // For some reason, adding a 1ms delay between these two lines fixes a crash when playing with mpv from downloads
                withContext(Dispatchers.IO) {
                    Thread.sleep(1)
                }
            }
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

        _uiState.update { it.copy(currentTrickPlay = null) }
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
        val introCheckRunnable = object : Runnable {
            override fun run() {
                if (player.currentMediaItem != null && player.currentMediaItem!!.mediaId.isNotEmpty()) {
                    val itemId = UUID.fromString(player.currentMediaItem!!.mediaId)
                    intros[itemId]?.let { intro ->
                        val seconds = player.currentPosition / 1000.0
                        if (seconds > intro.showSkipPromptAt && seconds < intro.hideSkipPromptAt) {
                            _uiState.update { it.copy(currentIntro = intro) }
                            return@let
                        }
                        _uiState.update { it.copy(currentIntro = null) }
                    }
                }
                handler.postDelayed(this, 1000L)
            }
        }
        handler.post(playbackProgressRunnable)
        if (intros.isNotEmpty()) handler.post(introCheckRunnable)
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
                        _uiState.update { it.copy(currentItemTitle = itemTitle) }

                        jellyfinRepository.postPlaybackStart(item.itemId)

                        if (appPreferences.playerTrickPlay) {
                            getTrickPlay(item.itemId)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e)
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
                currentAudioTracks.clear()
                currentSubtitleTracks.clear()
                when (player) {
                    is MPVPlayer -> {
                        player.currentMpvTracks.forEach {
                            when (it.type) {
                                TrackType.VIDEO -> Unit
                                TrackType.AUDIO -> {
                                    currentAudioTracks.add(it)
                                }
                                TrackType.SUBTITLE -> {
                                    currentSubtitleTracks.add(it)
                                }
                            }
                        }
                    }
                }
                _uiState.update { it.copy(fileLoaded = true) }
            }
            ExoPlayer.STATE_ENDED -> {
                stateString = "ExoPlayer.STATE_ENDED     -"
                _navigateBack.tryEmit(true)
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

    fun switchToTrack(trackType: TrackType, track: MPVPlayer.Companion.Track) {
        if (player is MPVPlayer) {
            player.selectTrack(trackType, id = track.id)
            disableSubtitle = track.ffIndex == -1
        }
    }

    fun selectSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }

    private suspend fun getTrickPlay(itemId: UUID) {
        if (trickPlays[itemId] != null) return
        jellyfinRepository.getTrickPlayManifest(itemId)
            ?.let { trickPlayManifest ->
                val widthResolution =
                    trickPlayManifest.widthResolutions.max()
                Timber.d("Trickplay Resolution: $widthResolution")

                jellyfinRepository.getTrickPlayData(
                    itemId,
                    widthResolution,
                )?.let { byteArray ->
                    val trickPlayData =
                        BifUtil.trickPlayDecode(byteArray, widthResolution)

                    trickPlayData?.let { bifData ->
                        Timber.d("Trickplay Images: ${bifData.imageCount}")
                        trickPlays[itemId] = bifData
                        _uiState.update { it.copy(currentTrickPlay = trickPlays[itemId]) }
                    }
                }
            }
    }
}
