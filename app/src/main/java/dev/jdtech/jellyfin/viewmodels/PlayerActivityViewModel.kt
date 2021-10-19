package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PlayerActivityViewModel
@Inject
constructor(
    application: Application,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel(), Player.Listener {
    val player: BasePlayer

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _currentItemTitle = MutableLiveData<String>()
    val currentItemTitle: LiveData<String> = _currentItemTitle

    var currentAudioTracks: MutableList<MPVPlayer.Companion.Track> = mutableListOf()
    var currentSubtitleTracks: MutableList<MPVPlayer.Companion.Track> = mutableListOf()

    private val _fileLoaded = MutableLiveData(false)
    val fileLoaded: LiveData<Boolean> = _fileLoaded

    private var items: Array<PlayerItem> = arrayOf()

    val trackSelector = DefaultTrackSelector(application)
    var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    var playbackSpeed: Float = 1f

    private val sp = PreferenceManager.getDefaultSharedPreferences(application)

    init {
        val useMpv = sp.getBoolean("mpv_player", false)

        val preferredAudioLanguage = sp.getString("audio_language", null) ?: ""
        val preferredSubtitleLanguage = sp.getString("subtitle_language", null) ?: ""

        if (useMpv) {
            val preferredLanguages = mapOf(
                TrackType.AUDIO to preferredAudioLanguage,
                TrackType.SUBTITLE to preferredSubtitleLanguage
            )
            player = MPVPlayer(
                application,
                false,
                preferredLanguages,
                sp.getBoolean("mpv_disable_hwdec", false)
            )
        } else {
            val renderersFactory =
                DefaultRenderersFactory(application).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                )
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTunnelingEnabled(true)
                    .setPreferredAudioLanguage(preferredAudioLanguage)
                    .setPreferredTextLanguage(preferredSubtitleLanguage)
            )
            player = SimpleExoPlayer.Builder(application, renderersFactory)
                .setTrackSelector(trackSelector)
                .build()
        }
    }

    fun initializePlayer(
        items: Array<PlayerItem>
    ) {
        this.items = items
        player.addListener(this)

        viewModelScope.launch {
            val mediaItems: MutableList<MediaItem> = mutableListOf()

            try {
                for (item in items) {
                    val streamUrl = jellyfinRepository.getStreamUrl(item.itemId, item.mediaSourceId)
                    Timber.d("Stream url: $streamUrl")
                    val mediaItem =
                        MediaItem.Builder()
                            .setMediaId(item.itemId.toString())
                            .setUri(streamUrl)
                            .build()
                    mediaItems.add(mediaItem)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            player.setMediaItems(mediaItems, currentWindow, items[0].playbackPosition)
            player.prepare()
            player.play()
        }

        pollPosition(player)
    }

    private fun releasePlayer() {
        player.let { player ->
            runBlocking {
                try {
                    jellyfinRepository.postPlaybackStop(
                        UUID.fromString(player.currentMediaItem?.mediaId),
                        player.currentPosition.times(10000)
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        playWhenReady = player.playWhenReady
        playbackPosition = player.currentPosition
        currentWindow = player.currentWindowIndex
        player.removeListener(this)
        player.release()
    }

    private fun pollPosition(player: BasePlayer) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                viewModelScope.launch {
                    if (player.currentMediaItem != null) {
                        try {
                            jellyfinRepository.postPlaybackProgress(
                                UUID.fromString(player.currentMediaItem!!.mediaId),
                                player.currentPosition.times(10000),
                                !player.isPlaying
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.d("Playing MediaItem: ${mediaItem?.mediaId}")
        viewModelScope.launch {
            try {
                for (item in items) {
                    if (item.itemId.toString() == player.currentMediaItem?.mediaId ?: "") {
                        _currentItemTitle.value = item.name
                    }
                }
                jellyfinRepository.postPlaybackStart(UUID.fromString(mediaItem?.mediaId))
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
                        player.currentTracks.forEach {
                            when (it.type) {
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
                _fileLoaded.value = true
            }
            ExoPlayer.STATE_ENDED -> {
                stateString = "ExoPlayer.STATE_ENDED     -"
                _navigateBack.value = true
            }
        }
        Timber.d("Changed player state to $stateString")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing Player ViewModel")
        releasePlayer()
    }

    fun switchToTrack(trackType: String, track: MPVPlayer.Companion.Track) {
        if (player is MPVPlayer) {
            player.selectTrack(trackType, isExternal = false, index = track.ffIndex)
        }
    }

    fun selectSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }
}