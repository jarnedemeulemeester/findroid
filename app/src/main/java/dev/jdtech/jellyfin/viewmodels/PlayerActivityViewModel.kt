package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PlayerActivityViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private var _player = MutableLiveData<SimpleExoPlayer>()
    var player: LiveData<SimpleExoPlayer> = _player


    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var _playbackStateListener: PlaybackStateListener

    val playbackStateListener: PlaybackStateListener
        get() = _playbackStateListener

    init {
        _playbackStateListener = PlaybackStateListener()
    }

    fun initializePlayer(itemId: UUID, mediaSourceId: String, playbackPosition: Long) {
        val renderersFactory =
            DefaultRenderersFactory(application).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val trackSelector = DefaultTrackSelector(application)
        trackSelector.setParameters(
            trackSelector.buildUponParameters().setTunnelingEnabled(true),
        )
        val player = SimpleExoPlayer.Builder(application, renderersFactory)
            .setTrackSelector(trackSelector)
            .build()

        player.addListener(_playbackStateListener)

        viewModelScope.launch {
            val streamUrl = jellyfinRepository.getStreamUrl(itemId, mediaSourceId)
            Log.d("PlayerActivity", streamUrl)
            val mediaItem =
                MediaItem.Builder()
                    .setMediaId(itemId.toString())
                    .setUri(streamUrl)
                    .build()
            player.setMediaItem(mediaItem, playbackPosition)
            player.playWhenReady = playWhenReady
            player.prepare()
            _player.value = player
        }
    }

    private fun releasePlayer() {
        if (player.value != null) {
            playWhenReady = player.value!!.playWhenReady
            playbackPosition = player.value!!.currentPosition
            currentWindow = player.value!!.currentWindowIndex
            player.value!!.removeListener(_playbackStateListener)
            player.value!!.release()
            _player.value = null
        }
    }

    class PlaybackStateListener : Player.Listener {
        private val _navigateBack = MutableLiveData<Boolean>()
        val navigateBack: LiveData<Boolean> = _navigateBack

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
            Log.d("PlayerActivity", "changed state to $stateString")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("PlayerActivity", "onCleared ViewModel")
        releasePlayer()
    }
}