package dev.jdtech.jellyfin.themesong

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.coroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoThemeSongPlayer @Inject constructor(
    @ApplicationContext
    context: Context,
    lifecycle: Lifecycle,
) : ThemeSongPlayer {

    private sealed interface Event {
        data class Play(val mediaUri: String) : Event
        data object Pause : Event
        data object Stop : Event
    }

    private val player = ExoPlayer.Builder(context).build()

    private val events = MutableSharedFlow<Event>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1,
    )

    init {
        with(lifecycle) {
            coroutineScope.launch(Dispatchers.Main) {
                events.collect { event ->
                    when (event) {
                        is Event.Play -> playThemeSong(event)
                        is Event.Pause -> pauseThemeSong()
                        is Event.Stop -> stopThemeSong()
                    }
                }
            }
            addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        pause()
                    }
                },
            )
        }
    }

    override fun play(mediaUri: String) {
        events.tryEmit(Event.Play(mediaUri))
    }

    override fun pause() {
        events.tryEmit(Event.Pause)
    }

    override fun stop() {
        events.tryEmit(Event.Stop)
    }

    private suspend fun playThemeSong(event: Event.Play) {
        val mediaItem = MediaItem.fromUri(event.mediaUri)
        with(player) {
            if (currentMediaItem?.mediaId == mediaItem.mediaId) {
                // Resume if possible
                if (!isPlaying) {
                    play()
                    fadeIn()
                }
                return
            }

            setMediaItem(mediaItem)
            prepare()
            play()
            fadeIn()
        }
    }

    private suspend fun pauseThemeSong() {
        fadeOut()
        player.pause()
    }

    private suspend fun stopThemeSong() {
        fadeOut()
        player.stop()
        player.clearMediaItems()
    }

    private suspend fun fadeIn() {
        player.volume = 0f
        val steps = 10
        repeat(steps) {
            player.volume += 1f / steps
            delay(100)
        }
    }

    private suspend fun fadeOut() {
        player.volume = 1f
        val steps = 10
        repeat(steps) {
            player.volume -= 1f / steps
            delay(100)
        }
    }
}
