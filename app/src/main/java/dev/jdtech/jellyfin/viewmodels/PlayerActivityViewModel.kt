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
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel(), Player.Listener {
    private var _player = MutableLiveData<SimpleExoPlayer>()
    var player: LiveData<SimpleExoPlayer> = _player

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private val sp = PreferenceManager.getDefaultSharedPreferences(application)

    fun initializePlayer(
        items: Array<PlayerItem>,
        playbackPosition: Long
    ) {

        val renderersFactory =
            DefaultRenderersFactory(application).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val trackSelector = DefaultTrackSelector(application)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setTunnelingEnabled(true)
                .setPreferredAudioLanguage(sp.getString("audio_language", null))
                .setPreferredTextLanguage(sp.getString("subtitle_language", null))
        )
        val player = SimpleExoPlayer.Builder(application, renderersFactory)
            .setTrackSelector(trackSelector)
            .build()

        player.addListener(this)

        viewModelScope.launch {
            val mediaItems: MutableList<MediaItem> = mutableListOf()

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

            player.setMediaItems(mediaItems, currentWindow, playbackPosition)
            player.playWhenReady = playWhenReady
            player.prepare()
            _player.value = player
        }

        pollPosition(player)
    }

    private fun releasePlayer() {
        _player.value?.let { player ->
            runBlocking {
                jellyfinRepository.postPlaybackStop(
                    UUID.fromString(player.currentMediaItem?.mediaId),
                    player.currentPosition.times(10000)
                )
            }
        }

        if (player.value != null) {
            playWhenReady = player.value!!.playWhenReady
            playbackPosition = player.value!!.currentPosition
            currentWindow = player.value!!.currentWindowIndex
            player.value!!.removeListener(this)
            player.value!!.release()
            _player.value = null
        }
    }

    private fun pollPosition(player: SimpleExoPlayer) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                viewModelScope.launch {
                    if (player.currentMediaItem != null) {
                        jellyfinRepository.postPlaybackProgress(
                            UUID.fromString(player.currentMediaItem!!.mediaId),
                            player.currentPosition.times(10000),
                            !player.isPlaying
                        )
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
            jellyfinRepository.postPlaybackStart(UUID.fromString(mediaItem?.mediaId))
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
}