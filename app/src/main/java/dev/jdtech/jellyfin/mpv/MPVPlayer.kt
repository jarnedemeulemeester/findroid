package dev.jdtech.jellyfin.mpv

import `is`.xyz.libmpv.MPVLib
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.core.content.getSystemService
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.device.DeviceInfo
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.*
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArraySet

@Suppress("SpellCheckingInspection")
class MPVPlayer(
    context: Context,
    requestAudioFocus: Boolean,
    preferredLanguages: Map<String, String>
) : BasePlayer(), MPVLib.EventObserver, AudioManager.OnAudioFocusChangeListener {

    private val audioManager: AudioManager by lazy { context.getSystemService()!! }
    private var audioFocusCallback: () -> Unit = {}
    private var audioFocusRequest = AudioManager.AUDIOFOCUS_REQUEST_FAILED
    private val handler = Handler(context.mainLooper)

    init {
        require(context is Application)
        @Suppress("DEPRECATION")
        val mpvDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "mpv")
        if (!mpvDir.exists()) {
            mpvDir.mkdirs()
        }
        // https://github.com/mpv-android/mpv-android/commit/12d4d78
        arrayOf("mpv.conf", "subfont.ttf"/*, "cacert.pem"*/).forEach { fileName ->
            val file = File(mpvDir, fileName)
            Log.i("mpv", "File ${file.absolutePath}")
            if (!file.exists()) {
                context.assets.open(fileName, AssetManager.ACCESS_STREAMING).copyTo(FileOutputStream(file))
            }
        }
        MPVLib.create(context)
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", mpvDir.path)
        // vo: set display fps as reported by android
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("ao", "audiotrack,opensles")

        MPVLib.init()

        // hardcoded options
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-pause-initial", "yes")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("keep-open", "always")
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("sub-font-provider", "none")
        MPVLib.setOptionString("ytdl", "no")

        for (preferredLanguage in preferredLanguages) {
            when (preferredLanguage.key) {
                TrackType.AUDIO -> {
                    MPVLib.setOptionString("alang", preferredLanguage.value)
                }
                TrackType.SUBTITLE -> {
                    MPVLib.setOptionString("slang", preferredLanguage.value)
                }
            }
        }

        MPVLib.addObserver(this)

        // Observe properties
        data class Property(val name: String, @MPVLib.Format val format: Int)
        arrayOf(
            Property("track-list", MPVLib.MPV_FORMAT_STRING),
            Property("paused-for-cache", MPVLib.MPV_FORMAT_FLAG),
            Property("eof-reached", MPVLib.MPV_FORMAT_FLAG),
            Property("seekable", MPVLib.MPV_FORMAT_FLAG),
            Property("time-pos", MPVLib.MPV_FORMAT_INT64),
            Property("duration", MPVLib.MPV_FORMAT_INT64),
            Property("demuxer-cache-time", MPVLib.MPV_FORMAT_INT64),
            Property("speed", MPVLib.MPV_FORMAT_DOUBLE)
        ).forEach { (name, format) ->
            MPVLib.observeProperty(name, format)
        }

        if (requestAudioFocus) {
            @Suppress("DEPRECATION")
            audioFocusRequest = audioManager.requestAudioFocus(
                /* listener= */ this,
                /* streamType= */ AudioManager.STREAM_MUSIC,
                /* durationHint= */ AudioManager.AUDIOFOCUS_GAIN
            )
            if (audioFocusRequest != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MPVLib.setPropertyBoolean("pause", true)
            }
        }
    }

    // Listeners and notification.
    @Suppress("DEPRECATION")
    private val listeners: ListenerSet<Player.EventListener> = ListenerSet(
        context.mainLooper,
        Clock.DEFAULT
    ) { listener: Player.EventListener, flags: FlagSet ->
        listener.onEvents( /* player= */this, Player.Events(flags))
    }
    @Suppress("DEPRECATION")
    private val videoListeners = CopyOnWriteArraySet<com.google.android.exoplayer2.video.VideoListener>()

    // Internal state.
    private var internalMediaItems: List<MediaItem>? = null
    private var internalMediaItem: MediaItem? = null
    @Player.State
    private var playbackState: Int = Player.STATE_IDLE
    private var currentPlayWhenReady: Boolean = false
    @Player.RepeatMode
    private val repeatMode: Int = REPEAT_MODE_OFF
    private var trackGroupArray: TrackGroupArray = TrackGroupArray.EMPTY
    private var trackSelectionArray: TrackSelectionArray = TrackSelectionArray()
    private var playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT

    // MPV Custom
    private var isPlayerReady: Boolean = false
    private var isSeekable: Boolean = false
    private var currentPositionMs: Long? = null
    private var currentDurationMs: Long? = null
    private var currentCacheDurationMs: Long? = null
    var currentTracks: List<Track> = emptyList()
    private var initialCommands = mutableListOf<Array<String>>()
    private var initialSeekTo: Long = 0L

    // mpv events
    override fun eventProperty(property: String) {
        // Nothing to do...
    }

    override fun eventProperty(property: String, value: String) {
        handler.post {
            when (property) {
                "track-list" -> {
                    val (tracks, newTrackGroupArray, newTrackSelectionArray) = getMPVTracks(value)
                    tracks.forEach { Log.i("mpv", "${it.ffIndex} ${it.type} ${it.codec}") }
                    currentTracks = tracks
                    if (isPlayerReady) {
                        if (newTrackGroupArray != trackGroupArray || newTrackSelectionArray != trackSelectionArray) {
                            trackGroupArray = newTrackGroupArray
                            trackSelectionArray = newTrackSelectionArray
                            listeners.sendEvent(Player.EVENT_TRACKS_CHANGED) { listener ->
                                listener.onTracksChanged(currentTrackGroups, currentTrackSelections)
                            }
                        }
                    } else {
                        trackGroupArray = newTrackGroupArray
                        trackSelectionArray = newTrackSelectionArray
                    }
                }
            }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        handler.post {
            when (property) {
                "eof-reached" -> {
                    if (value && isPlayerReady) {
                        setPlayerStateAndNotifyIfChanged(
                            playWhenReady = false,
                            playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM,
                            playbackState = Player.STATE_ENDED
                        )
                        resetInternalState()
                    }
                }
                "paused-for-cache" -> {
                    if (isPlayerReady) {
                        if (value) {
                            setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_BUFFERING)
                        } else {
                            setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_READY)
                        }
                    }
                }
                "seekable" -> {
                    if (isSeekable != value) {
                        isSeekable = value
                        listeners.sendEvent(Player.EVENT_TIMELINE_CHANGED) { listener ->
                            listener.onTimelineChanged(timeline, Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE)
                        }
                    }
                }
            }
        }
    }

    override fun eventProperty(property: String, value: Long) {
        handler.post {
            when (property) {
                "time-pos" -> currentPositionMs = value * C.MILLIS_PER_SECOND
                "duration" -> {
                    if (currentDurationMs != value * C.MILLIS_PER_SECOND) {
                        currentDurationMs = value * C.MILLIS_PER_SECOND
                        listeners.sendEvent(Player.EVENT_TIMELINE_CHANGED) { listener ->
                            listener.onTimelineChanged(timeline, Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE)
                        }
                    }
                }
                "demuxer-cache-time" -> currentCacheDurationMs = value * C.MILLIS_PER_SECOND
            }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        handler.post {
            when (property) {
                "speed" -> {
                    playbackParameters = getPlaybackParameters().withSpeed(value.toFloat())
                    listeners.sendEvent(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED) {listener ->
                        listener.onPlaybackParametersChanged(getPlaybackParameters())
                    }
                }
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun event(@MPVLib.Event eventId: Int) {
        handler.post {
            when (eventId) {
                MPVLib.MPV_EVENT_START_FILE -> {
                    if (!isPlayerReady) {
                        for (command in initialCommands) {
                            MPVLib.command(command)
                        }
                    }
                }
                MPVLib.MPV_EVENT_SEEK -> {
                    setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_BUFFERING)
                    listeners.sendEvent(Player.EVENT_POSITION_DISCONTINUITY) { listener ->
                        @Suppress("DEPRECATION")
                        listener.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_SEEK)
                    }
                }
                MPVLib.MPV_EVENT_PLAYBACK_RESTART -> {
                    if (!isPlayerReady) {
                        isPlayerReady = true
                        listeners.sendEvent(Player.EVENT_TRACKS_CHANGED) { listener ->
                            listener.onTracksChanged(currentTrackGroups, currentTrackSelections)
                        }
                        seekTo(C.TIME_UNSET)
                        if (playWhenReady) {
                            MPVLib.setPropertyBoolean("pause", false)
                        }
                        for (videoListener in videoListeners) {
                            videoListener.onRenderedFirstFrame()
                        }
                    } else {
                        if (playbackState == Player.STATE_BUFFERING && bufferedPosition > currentPosition) {
                            setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_READY)
                        }
                    }
                }
            }
        }
    }

    override fun eventEndFile(@MPVLib.Reason reason: Int, @MPVLib.Error error: Int) {
        // Nothing to do...
    }

    private fun setPlayerStateAndNotifyIfChanged(
        playWhenReady: Boolean = getPlayWhenReady(),
        @Player.PlayWhenReadyChangeReason playWhenReadyChangeReason: Int = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        @Player.State playbackState: Int = getPlaybackState()
    ) {
        var playerStateChanged = false
        val wasPlaying = isPlaying
        if (playbackState != getPlaybackState()) {
            this.playbackState = playbackState
            listeners.queueEvent(Player.EVENT_PLAYBACK_STATE_CHANGED) { listener ->
                listener.onPlaybackStateChanged(playbackState)
            }
            playerStateChanged = true
        }
        if (playWhenReady != getPlayWhenReady()) {
            this.currentPlayWhenReady = playWhenReady
            listeners.queueEvent(Player.EVENT_PLAY_WHEN_READY_CHANGED) { listener ->
                listener.onPlayWhenReadyChanged(playWhenReady, playWhenReadyChangeReason)
            }
            playerStateChanged = true
        }
        if (playerStateChanged) {
            listeners.queueEvent( /* eventFlag= */ C.INDEX_UNSET) { listener ->
                @Suppress("DEPRECATION")
                listener.onPlayerStateChanged(playWhenReady, playbackState)
            }
        }
        if (wasPlaying != isPlaying) {
            listeners.queueEvent(Player.EVENT_IS_PLAYING_CHANGED) { listener ->
                listener.onIsPlayingChanged(isPlaying)
            }
        }
        listeners.flushEvents()
    }

    /**
     * Select a [Track] or disable a [TrackType] in the current player.
     *
     * @param trackType The [TrackType]
     * @param isExternal If track is external or embed in media
     * @param index Index to select or [C.INDEX_UNSET] to disable [TrackType]
     * @return true if the track is or was already selected
     */
    fun selectTrack(@TrackType trackType: String, isExternal: Boolean = false, index: Int): Boolean {
        if (index != C.INDEX_UNSET) {
            Log.i("mpv", "${currentTracks.size}")
            currentTracks.firstOrNull {
                it.type == trackType && (if (isExternal) it.title else "${it.ffIndex}") == "$index"
            }.let { track ->
                if (track != null) {
                    Log.i("mpv", "selected track ${track.ffIndex} ${track.type}")
                    if (!track.selected) {
                        MPVLib.setPropertyInt(trackType, track.id)
                    }
                } else {
                    return false
                }
            }
        } else {
            if (currentTracks.indexOfFirst { it.type == trackType && it.selected } != C.INDEX_UNSET) {
                MPVLib.setPropertyString(trackType, "no")
            }
        }
        return true
    }

    // Timeline wrapper
    private val timeline: Timeline = object : Timeline() {
        /**
         * Returns the number of windows in the timeline.
         */
        override fun getWindowCount(): Int {
            return 1
        }

        /**
         * Populates a [com.google.android.exoplayer2.Timeline.Window] with data for the window at the specified index.
         *
         * @param windowIndex The index of the window.
         * @param window The [com.google.android.exoplayer2.Timeline.Window] to populate. Must not be null.
         * @param defaultPositionProjectionUs A duration into the future that the populated window's
         * default start position should be projected.
         * @return The populated [com.google.android.exoplayer2.Timeline.Window], for convenience.
         */
        override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
            val currentMediaItem = internalMediaItem ?: MediaItem.Builder().build()
            return if (windowIndex == 0) window.set(
                /* uid= */ 0,
                /* mediaItem= */ currentMediaItem,
                /* manifest= */ null,
                /* presentationStartTimeMs= */ C.TIME_UNSET,
                /* windowStartTimeMs= */ C.TIME_UNSET,
                /* elapsedRealtimeEpochOffsetMs= */ C.TIME_UNSET,
                /* isSeekable= */ isSeekable,
                /* isDynamic= */ !isSeekable,
                /* liveConfiguration= */ currentMediaItem.liveConfiguration,
                /* defaultPositionUs= */ C.TIME_UNSET,
                /* durationUs= */ C.msToUs(currentDurationMs ?: C.TIME_UNSET),
                /* firstPeriodIndex= */ windowIndex,
                /* lastPeriodIndex= */ windowIndex,
                /* positionInFirstPeriodUs= */ C.TIME_UNSET
            ) else window
        }

        /**
         * Returns the number of periods in the timeline.
         */
        override fun getPeriodCount(): Int {
            return 1
        }

        /**
         * Populates a [com.google.android.exoplayer2.Timeline.Period] with data for the period at the specified index.
         *
         * @param periodIndex The index of the period.
         * @param period The [com.google.android.exoplayer2.Timeline.Period] to populate. Must not be null.
         * @param setIds Whether [com.google.android.exoplayer2.Timeline.Period.id] and [com.google.android.exoplayer2.Timeline.Period.uid] should be populated. If false,
         * the fields will be set to null. The caller should pass false for efficiency reasons unless
         * the fields are required.
         * @return The populated [com.google.android.exoplayer2.Timeline.Period], for convenience.
         */
        override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
            return if (periodIndex == 0) period.set(
                /* id= */ 0,
                /* uid= */ 0,
                /* windowIndex= */ periodIndex,
                /* durationUs= */ C.msToUs(currentDurationMs ?: C.TIME_UNSET),
                /* positionInWindowUs= */ 0
            ) else period
        }

        /**
         * Returns the index of the period identified by its unique [com.google.android.exoplayer2.Timeline.Period.uid], or [ ][C.INDEX_UNSET] if the period is not in the timeline.
         *
         * @param uid A unique identifier for a period.
         * @return The index of the period, or [C.INDEX_UNSET] if the period was not found.
         */
        override fun getIndexOfPeriod(uid: Any): Int {
            return if (uid == 0) 0 else C.INDEX_UNSET
        }

        /**
         * Returns the unique id of the period identified by its index in the timeline.
         *
         * @param periodIndex The index of the period.
         * @return The unique id of the period.
         */
        override fun getUidOfPeriod(periodIndex: Int): Any {
            return if (periodIndex == 0) 0 else C.INDEX_UNSET
        }
    }

    // OnAudioFocusChangeListener implementation.

    /**
     * Called on the listener to notify it the audio focus for this listener has been changed.
     * The focusChange value indicates whether the focus was gained,
     * whether the focus was lost, and whether that loss is transient, or whether the new focus
     * holder will hold it for an unknown amount of time.
     * When losing focus, listeners can use the focus change information to decide what
     * behavior to adopt when losing focus. A music player could for instance elect to lower
     * the volume of its music stream (duck) for transient focus losses, and pause otherwise.
     * @param focusChange the type of focus change, one of [AudioManager.AUDIOFOCUS_GAIN],
     * [AudioManager.AUDIOFOCUS_LOSS], [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT]
     * and [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK].
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val oldAudioFocusCallback = audioFocusCallback
                val wasPlaying = isPlaying
                MPVLib.setPropertyBoolean("pause", true)
                setPlayerStateAndNotifyIfChanged(
                    playWhenReady = false,
                    playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
                )
                audioFocusCallback = {
                    oldAudioFocusCallback()
                    if (wasPlaying) MPVLib.setPropertyBoolean("pause", false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                MPVLib.command(arrayOf("multiply", "volume", "$AUDIO_FOCUS_DUCKING"))
                audioFocusCallback = {
                    MPVLib.command(arrayOf("multiply", "volume", "${1f/AUDIO_FOCUS_DUCKING}"))
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusCallback()
                audioFocusCallback = {}
            }
        }
    }

    // Player implementation.

    /**
     * Returns the [Looper] associated with the application thread that's used to access the
     * player and on which player events are received.
     */
    override fun getApplicationLooper(): Looper {
        return handler.looper
    }

    /**
     * Registers a listener to receive events from the player. The listener's methods will be called
     * on the thread that was used to construct the player. However, if the thread used to construct
     * the player does not have a [Looper], then the listener will be called on the main thread.
     *
     * @param listener The listener to register.
     */
    @Suppress("DEPRECATION")
    override fun addListener(listener: Player.EventListener) {
        listeners.add(listener)
    }

    /**
     * Registers a listener to receive all events from the player.
     *
     * @param listener The listener to register.
     */
    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
        videoListeners.add(listener)
    }

    /**
     * Unregister a listener registered through [.addListener]. The listener will
     * no longer receive events from the player.
     *
     * @param listener The listener to unregister.
     */
    @Suppress("DEPRECATION")
    override fun removeListener(listener: Player.EventListener) {
        listeners.remove(listener)
    }

    /**
     * Unregister a listener registered through [.addListener]. The listener will no
     * longer receive events.
     *
     * @param listener The listener to unregister.
     */
    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
        videoListeners.remove(listener)
    }

    /**
     * Clears the playlist and adds the specified [MediaItems][MediaItem].
     *
     * @param mediaItems The new [MediaItems][MediaItem].
     * @param resetPosition Whether the playback position should be reset to the default position in
     * the first [Timeline.Window]. If false, playback will start from the position defined
     * by [.getCurrentWindowIndex] and [.getCurrentPosition].
     */
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        internalMediaItems = mediaItems
    }

    /**
     * Clears the playlist and adds the specified [MediaItems][MediaItem].
     *
     * @param mediaItems The new [MediaItems][MediaItem].
     * @param startWindowIndex The window index to start playback from. If [com.google.android.exoplayer2.C.INDEX_UNSET] is
     * passed, the current position is not reset.
     * @param startPositionMs The position in milliseconds to start playback from. If [     ][com.google.android.exoplayer2.C.TIME_UNSET] is passed, the default position of the given window is used. In any case, if
     * `startWindowIndex` is set to [com.google.android.exoplayer2.C.INDEX_UNSET], this parameter is ignored and the
     * position is not reset at all.
     * @throws com.google.android.exoplayer2.IllegalSeekPositionException If the provided `startWindowIndex` is not within the
     * bounds of the list of media items.
     */
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startWindowIndex: Int, startPositionMs: Long) {
        TODO("Not yet implemented")
    }

    /**
     * Adds a list of media items at the given index of the playlist.
     *
     * @param index The index at which to add the media items. If the index is larger than the size of
     * the playlist, the media items are added to the end of the playlist.
     * @param mediaItems The [MediaItems][MediaItem] to add.
     */
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        TODO("Not yet implemented")
    }

    /**
     * Moves the media item range to the new index.
     *
     * @param fromIndex The start of the range to move.
     * @param toIndex The first item not to be included in the range (exclusive).
     * @param newIndex The new index of the first media item of the range. If the new index is larger
     * than the size of the remaining playlist after removing the range, the range is moved to the
     * end of the playlist.
     */
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Removes a range of media items from the playlist.
     *
     * @param fromIndex The index at which to start removing media items.
     * @param toIndex The index of the first item to be kept (exclusive). If the index is larger than
     * the size of the playlist, media items to the end of the playlist are removed.
     */
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the player's currently available [com.google.android.exoplayer2.Player.Commands].
     *
     *
     * The returned [com.google.android.exoplayer2.Player.Commands] are not updated when available commands change. Use [ ][com.google.android.exoplayer2.Player.Listener.onAvailableCommandsChanged] to get an update when the available commands
     * change.
     *
     *
     * Executing a command that is not available (for example, calling [.next] if [ ][.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM] is unavailable) will neither throw an exception nor generate
     * a [.getPlayerError] player error}.
     *
     *
     * [.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM] and [.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM]
     * are unavailable if there is no such [MediaItem].
     *
     * @return The currently available [com.google.android.exoplayer2.Player.Commands].
     * @see com.google.android.exoplayer2.Player.Listener.onAvailableCommandsChanged
     */
    override fun getAvailableCommands(): Player.Commands {
        return permanentAvailableCommands
    }

    private fun resetInternalState() {
        isPlayerReady = false
        isSeekable = false
        playbackState = Player.STATE_IDLE
        currentPlayWhenReady = false
        currentPositionMs = null
        currentDurationMs = null
        currentCacheDurationMs = null
        trackGroupArray = TrackGroupArray.EMPTY
        trackSelectionArray = TrackSelectionArray()
        playbackParameters = PlaybackParameters.DEFAULT
        initialCommands.clear()
        initialSeekTo = 0L
    }

    /** Prepares the player.  */
    override fun prepare() {
        internalMediaItems?.firstOrNull { it.playbackProperties?.uri != null }?.let { mediaItem ->
            internalMediaItem = mediaItem
            resetInternalState()
            mediaItem.playbackProperties?.subtitles?.forEach { subtitle ->
                initialCommands.add(arrayOf(
                    /* command= */ "sub-add",
                    /* url= */ "${subtitle.uri}",
                    /* flags= */ "auto",
                    /* title= */ "${subtitle.label}",
                    /* lang= */ "${subtitle.language}"
                ))
            }
            MPVLib.command(arrayOf("loadfile", "${mediaItem.playbackProperties?.uri}"))
            MPVLib.setPropertyBoolean("pause", true)
            listeners.sendEvent(Player.EVENT_TIMELINE_CHANGED) { listener ->
                listener.onTimelineChanged(timeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            }
            listeners.sendEvent(Player.EVENT_MEDIA_ITEM_TRANSITION) { listener ->
                listener.onMediaItemTransition(mediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
            }
            setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_BUFFERING)
        }
    }

    /**
     * Returns the current [playback state][com.google.android.exoplayer2.Player.State] of the player.
     *
     * @return The current [playback state][com.google.android.exoplayer2.Player.State].
     * @see com.google.android.exoplayer2.Player.Listener.onPlaybackStateChanged
     */
    override fun getPlaybackState(): Int {
        return playbackState
    }

    /**
     * Returns the reason why playback is suppressed even though [.getPlayWhenReady] is `true`, or [.PLAYBACK_SUPPRESSION_REASON_NONE] if playback is not suppressed.
     *
     * @return The current [playback suppression reason][com.google.android.exoplayer2.Player.PlaybackSuppressionReason].
     * @see com.google.android.exoplayer2.Player.Listener.onPlaybackSuppressionReasonChanged
     */
    override fun getPlaybackSuppressionReason(): Int {
        return PLAYBACK_SUPPRESSION_REASON_NONE
    }

    /**
     * Returns the error that caused playback to fail. This is the same error that will have been
     * reported via [com.google.android.exoplayer2.Player.Listener.onPlayerError] at the time of failure. It
     * can be queried using this method until the player is re-prepared.
     *
     *
     * Note that this method will always return `null` if [.getPlaybackState] is not
     * [.STATE_IDLE].
     *
     * @return The error, or `null`.
     * @see com.google.android.exoplayer2.Player.Listener.onPlayerError
     */
    override fun getPlayerError(): ExoPlaybackException? {
        return null
    }

    /**
     * Sets whether playback should proceed when [.getPlaybackState] == [.STATE_READY].
     *
     *
     * If the player is already in the ready state then this method pauses and resumes playback.
     *
     * @param playWhenReady Whether playback should proceed when ready.
     */
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (currentPlayWhenReady != playWhenReady) {
            setPlayerStateAndNotifyIfChanged(
                playWhenReady = playWhenReady,
                playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
            )
            if (isPlayerReady) {
                MPVLib.setPropertyBoolean("pause", !playWhenReady)
            }
        }
    }

    /**
     * Whether playback will proceed when [.getPlaybackState] == [.STATE_READY].
     *
     * @return Whether playback will proceed when ready.
     * @see com.google.android.exoplayer2.Player.Listener.onPlayWhenReadyChanged
     */
    override fun getPlayWhenReady(): Boolean {
        return currentPlayWhenReady
    }

    /**
     * Sets the [com.google.android.exoplayer2.Player.RepeatMode] to be used for playback.
     *
     * @param repeatMode The repeat mode.
     */
    override fun setRepeatMode(repeatMode: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the current [com.google.android.exoplayer2.Player.RepeatMode] used for playback.
     *
     * @return The current repeat mode.
     * @see com.google.android.exoplayer2.Player.Listener.onRepeatModeChanged
     */
    override fun getRepeatMode(): Int {
        return repeatMode
    }

    /**
     * Sets whether shuffling of windows is enabled.
     *
     * @param shuffleModeEnabled Whether shuffling is enabled.
     */
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        TODO("Not yet implemented")
    }

    /**
     * Returns whether shuffling of windows is enabled.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onShuffleModeEnabledChanged
     */
    override fun getShuffleModeEnabled(): Boolean {
        return false
    }

    /**
     * Whether the player is currently loading the source.
     *
     * @return Whether the player is currently loading the source.
     * @see com.google.android.exoplayer2.Player.Listener.onIsLoadingChanged
     */
    override fun isLoading(): Boolean {
        return false
    }

    /**
     * Seeks to a position specified in milliseconds in the specified window.
     *
     * @param windowIndex The index of the window.
     * @param positionMs The seek position in the specified window, or [com.google.android.exoplayer2.C.TIME_UNSET] to seek to
     * the window's default position.
     * @throws com.google.android.exoplayer2.IllegalSeekPositionException If the player has a non-empty timeline and the provided
     * `windowIndex` is not within the bounds of the current timeline.
     */
    override fun seekTo(windowIndex: Int, positionMs: Long) {
        if (windowIndex == 0) {
            val seekTo = if (positionMs != C.TIME_UNSET) positionMs / C.MILLIS_PER_SECOND else initialSeekTo
            if (isPlayerReady) {
                MPVLib.command(arrayOf("seek", "$seekTo", "absolute"))
            } else {
                initialSeekTo = seekTo
            }
        }
    }

    override fun getSeekBackIncrement(): Long {
        return 5000
    }

    override fun getSeekForwardIncrement(): Long {
        return 15000
    }

    override fun getMaxSeekToPreviousPosition(): Int {
        TODO("Not yet implemented")
    }

    /**
     * Attempts to set the playback parameters. Passing [PlaybackParameters.DEFAULT] resets the
     * player to the default, which means there is no speed or pitch adjustment.
     *
     *
     * Playback parameters changes may cause the player to buffer. [ ][com.google.android.exoplayer2.Player.Listener.onPlaybackParametersChanged] will be called whenever the currently
     * active playback parameters change.
     *
     * @param playbackParameters The playback parameters.
     */
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        if (getPlaybackParameters().speed != playbackParameters.speed) {
            MPVLib.setPropertyDouble("speed", playbackParameters.speed.toDouble())
        }
    }

    /**
     * Returns the currently active playback parameters.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onPlaybackParametersChanged
     */
    override fun getPlaybackParameters(): PlaybackParameters {
        return playbackParameters
    }

    override fun stop(reset: Boolean) {
        MPVLib.command(arrayOf("stop", "keep-playlist"))
    }

    /**
     * Releases the player. This method must be called when the player is no longer required. The
     * player must not be used after calling this method.
     */
    override fun release() {
        if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
        resetInternalState()
        MPVLib.destroy()
    }

    /**
     * Returns the available track groups.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onTracksChanged
     */
    override fun getCurrentTrackGroups(): TrackGroupArray {
        return trackGroupArray
    }

    /**
     * Returns the current track selections.
     *
     *
     * A concrete implementation may include null elements if it has a fixed number of renderer
     * components, wishes to report a TrackSelection for each of them, and has one or more renderer
     * components that is not assigned any selected tracks.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onTracksChanged
     */
    override fun getCurrentTrackSelections(): TrackSelectionArray {
        return trackSelectionArray
    }

    /**
     * Returns the current static metadata for the track selections.
     *
     *
     * The returned `metadataList` is an immutable list of [Metadata] instances, where
     * the elements correspond to the [current track selections][.getCurrentTrackSelections],
     * or an empty list if there are no track selections or the selected tracks contain no static
     * metadata.
     *
     *
     * This metadata is considered static in that it comes from the tracks' declared Formats,
     * rather than being timed (or dynamic) metadata, which is represented within a metadata track.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onStaticMetadataChanged
     */
    override fun getCurrentStaticMetadata(): List<Metadata> {
        return emptyList()
    }

    /**
     * Returns the current combined [MediaMetadata], or [MediaMetadata.EMPTY] if not
     * supported.
     *
     *
     * This [MediaMetadata] is a combination of the [MediaItem.mediaMetadata] and the
     * static and dynamic metadata sourced from [com.google.android.exoplayer2.Player.Listener.onStaticMetadataChanged] and
     * [com.google.android.exoplayer2.metadata.MetadataOutput.onMetadata].
     */
    override fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.EMPTY
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        TODO("Not yet implemented")
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the current [Timeline]. Never null, but may be empty.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onTimelineChanged
     */
    override fun getCurrentTimeline(): Timeline {
        return timeline
    }

    /** Returns the index of the period currently being played.  */
    override fun getCurrentPeriodIndex(): Int {
        return currentWindowIndex
    }

    /**
     * Returns the index of the current [window][Timeline.Window] in the [ ][.getCurrentTimeline], or the prospective window index if the [ ][.getCurrentTimeline] is empty.
     */
    override fun getCurrentWindowIndex(): Int {
        return timeline.getFirstWindowIndex(shuffleModeEnabled)
    }

    /**
     * Returns the duration of the current content window or ad in milliseconds, or [ ][com.google.android.exoplayer2.C.TIME_UNSET] if the duration is not known.
     */
    override fun getDuration(): Long {
        return timeline.getWindow(currentWindowIndex, window).durationMs
    }

    /**
     * Returns the playback position in the current content window or ad, in milliseconds, or the
     * prospective position in milliseconds if the [current timeline][.getCurrentTimeline] is
     * empty.
     */
    override fun getCurrentPosition(): Long {
        return currentPositionMs ?: C.TIME_UNSET
    }

    /**
     * Returns an estimate of the position in the current content window or ad up to which data is
     * buffered, in milliseconds.
     */
    override fun getBufferedPosition(): Long {
        return currentCacheDurationMs ?: contentPosition
    }

    /**
     * Returns an estimate of the total buffered duration from the current position, in milliseconds.
     * This includes pre-buffered data for subsequent ads and windows.
     */
    override fun getTotalBufferedDuration(): Long {
        return bufferedPosition
    }

    /** Returns whether the player is currently playing an ad.  */
    override fun isPlayingAd(): Boolean {
        return false
    }

    /**
     * If [.isPlayingAd] returns true, returns the index of the ad group in the period
     * currently being played. Returns [com.google.android.exoplayer2.C.INDEX_UNSET] otherwise.
     */
    override fun getCurrentAdGroupIndex(): Int {
        return C.INDEX_UNSET
    }

    /**
     * If [.isPlayingAd] returns true, returns the index of the ad in its ad group. Returns
     * [com.google.android.exoplayer2.C.INDEX_UNSET] otherwise.
     */
    override fun getCurrentAdIndexInAdGroup(): Int {
        return C.INDEX_UNSET
    }

    /**
     * If [.isPlayingAd] returns `true`, returns the content position that will be
     * played once all ads in the ad group have finished playing, in milliseconds. If there is no ad
     * playing, the returned position is the same as that returned by [.getCurrentPosition].
     */
    override fun getContentPosition(): Long {
        return currentPosition
    }

    /**
     * If [.isPlayingAd] returns `true`, returns an estimate of the content position in
     * the current content window up to which data is buffered, in milliseconds. If there is no ad
     * playing, the returned position is the same as that returned by [.getBufferedPosition].
     */
    override fun getContentBufferedPosition(): Long {
        return bufferedPosition
    }

    /** Returns the attributes for audio playback.  */
    override fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.DEFAULT
    }

    /**
     * Sets the audio volume, with 0 being silence and 1 being unity gain (signal unchanged).
     *
     * @param audioVolume Linear output gain to apply to all audio channels.
     */
    override fun setVolume(audioVolume: Float) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the audio volume, with 0 being silence and 1 being unity gain (signal unchanged).
     *
     * @return The linear gain applied to all audio channels.
     */
    override fun getVolume(): Float {
        TODO("Not yet implemented")
    }

    /**
     * Clears any [Surface], [SurfaceHolder], [SurfaceView] or [TextureView]
     * currently set on the player.
     */
    override fun clearVideoSurface() {
        TODO("Not yet implemented")
    }

    /**
     * Clears the [Surface] onto which video is being rendered if it matches the one passed.
     * Else does nothing.
     *
     * @param surface The surface to clear.
     */
    override fun clearVideoSurface(surface: Surface?) {
        TODO("Not yet implemented")
    }

    /**
     * Sets the [Surface] onto which video will be rendered. The caller is responsible for
     * tracking the lifecycle of the surface, and must clear the surface by calling `setVideoSurface(null)` if the surface is destroyed.
     *
     *
     * If the surface is held by a [SurfaceView], [TextureView] or [ ] then it's recommended to use [.setVideoSurfaceView], [ ][.setVideoTextureView] or [.setVideoSurfaceHolder] rather than
     * this method, since passing the holder allows the player to track the lifecycle of the surface
     * automatically.
     *
     * @param surface The [Surface].
     */
    override fun setVideoSurface(surface: Surface?) {
        TODO("Not yet implemented")
    }

    /**
     * Sets the [SurfaceHolder] that holds the [Surface] onto which video will be
     * rendered. The player will track the lifecycle of the surface automatically.
     *
     * @param surfaceHolder The surface holder.
     */
    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }

    /**
     * Clears the [SurfaceHolder] that holds the [Surface] onto which video is being
     * rendered if it matches the one passed. Else does nothing.
     *
     * @param surfaceHolder The surface holder to clear.
     */
    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }

    /**
     * Sets the [SurfaceView] onto which video will be rendered. The player will track the
     * lifecycle of the surface automatically.
     *
     * @param surfaceView The surface view.
     */
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        surfaceView?.holder?.addCallback(surfaceHolder)
    }

    /**
     * Clears the [SurfaceView] onto which video is being rendered if it matches the one passed.
     * Else does nothing.
     *
     * @param surfaceView The texture view to clear.
     */
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        surfaceView?.holder?.removeCallback(surfaceHolder)
    }

    /**
     * Sets the [TextureView] onto which video will be rendered. The player will track the
     * lifecycle of the surface automatically.
     *
     * @param textureView The texture view.
     */
    override fun setVideoTextureView(textureView: TextureView?) {
        TODO("Not yet implemented")
    }

    /**
     * Clears the [TextureView] onto which video is being rendered if it matches the one passed.
     * Else does nothing.
     *
     * @param textureView The texture view to clear.
     */
    override fun clearVideoTextureView(textureView: TextureView?) {
        TODO("Not yet implemented")
    }

    /**
     * Gets the size of the video.
     *
     *
     * The video's width and height are `0` if there is no video or its size has not been
     * determined yet.
     *
     * @see com.google.android.exoplayer2.Player.Listener.onVideoSizeChanged
     */
    override fun getVideoSize(): VideoSize {
        return VideoSize.UNKNOWN
    }

    /** Returns the current [Cues][Cue]. This list may be empty.  */
    override fun getCurrentCues(): MutableList<Cue> {
        TODO("Not yet implemented")
    }

    /** Gets the device information.  */
    override fun getDeviceInfo(): DeviceInfo {
        TODO("Not yet implemented")
    }

    /**
     * Gets the current volume of the device.
     *
     *
     * For devices with [local playback][DeviceInfo.PLAYBACK_TYPE_LOCAL], the volume returned
     * by this method varies according to the current [stream type][com.google.android.exoplayer2.C.StreamType]. The stream
     * type is determined by [AudioAttributes.usage] which can be converted to stream type with
     * [Util.getStreamTypeForAudioUsage].
     *
     *
     * For devices with [remote playback][DeviceInfo.PLAYBACK_TYPE_REMOTE], the volume of the
     * remote device is returned.
     */
    override fun getDeviceVolume(): Int {
        TODO("Not yet implemented")
    }

    /** Gets whether the device is muted or not.  */
    override fun isDeviceMuted(): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Sets the volume of the device.
     *
     * @param volume The volume to set.
     */
    override fun setDeviceVolume(volume: Int) {
        TODO("Not yet implemented")
    }

    /** Increases the volume of the device.  */
    override fun increaseDeviceVolume() {
        TODO("Not yet implemented")
    }

    /** Decreases the volume of the device.  */
    override fun decreaseDeviceVolume() {
        TODO("Not yet implemented")
    }

    /** Sets the mute state of the device.  */
    override fun setDeviceMuted(muted: Boolean) {
        TODO("Not yet implemented")
    }

    private class CurrentTrackSelection(
        private val currentTrackGroup: TrackGroup,
        private val index: Int
    ) : TrackSelection {
        /**
         * Returns an integer specifying the type of the selection, or [.TYPE_UNSET] if not
         * specified.
         *
         *
         * Track selection types are specific to individual applications, but should be defined
         * starting from [.TYPE_CUSTOM_BASE] to ensure they don't conflict with any types that may
         * be added to the library in the future.
         */
        override fun getType(): Int {
            return TrackSelection.TYPE_UNSET
        }

        /** Returns the [TrackGroup] to which the selected tracks belong.  */
        override fun getTrackGroup(): TrackGroup {
            return currentTrackGroup
        }

        /** Returns the number of tracks in the selection.  */
        override fun length(): Int {
            return if (index != C.INDEX_UNSET) 1 else 0
        }

        /**
         * Returns the format of the track at a given index in the selection.
         *
         * @param index The index in the selection.
         * @return The format of the selected track.
         */
        override fun getFormat(index: Int): Format {
            return currentTrackGroup.getFormat(index)
        }

        /**
         * Returns the index in the track group of the track at a given index in the selection.
         *
         * @param index The index in the selection.
         * @return The index of the selected track.
         */
        override fun getIndexInTrackGroup(index: Int): Int {
            return index
        }

        /**
         * Returns the index in the selection of the track with the specified format. The format is
         * located by identity so, for example, `selection.indexOf(selection.getFormat(index)) ==
         * index` even if multiple selected tracks have formats that contain the same values.
         *
         * @param format The format.
         * @return The index in the selection, or [C.INDEX_UNSET] if the track with the specified
         * format is not part of the selection.
         */
        override fun indexOf(format: Format): Int {
            return currentTrackGroup.indexOf(format)
        }

        /**
         * Returns the index in the selection of the track with the specified index in the track group.
         *
         * @param indexInTrackGroup The index in the track group.
         * @return The index in the selection, or [C.INDEX_UNSET] if the track with the specified
         * index is not part of the selection.
         */
        override fun indexOf(indexInTrackGroup: Int): Int {
            return indexInTrackGroup
        }
    }

    companion object {
        /**
         * Fraction to which audio volume is ducked on loss of audio focus
         */
        private const val AUDIO_FOCUS_DUCKING = 0.5f

        private val permanentAvailableCommands: Player.Commands = Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_SEEK_IN_CURRENT_WINDOW,
                COMMAND_PREPARE_STOP,
                COMMAND_SET_SPEED_AND_PITCH,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                //COMMAND_GET_MEDIA_ITEMS,
                COMMAND_GET_MEDIA_ITEMS_METADATA,
                COMMAND_CHANGE_MEDIA_ITEMS,
                COMMAND_SET_VIDEO_SURFACE,
                COMMAND_SEEK_FORWARD
            )
            .build()

        private val surfaceHolder: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            /**
             * This is called immediately after the surface is first created.
             * Implementations of this should start up whatever rendering code
             * they desire.  Note that only one thread can ever draw into
             * a [Surface], so you should not draw into the Surface here
             * if your normal rendering will be in another thread.
             *
             * @param holder The SurfaceHolder whose surface is being created.
             */
            override fun surfaceCreated(holder: SurfaceHolder) {
                MPVLib.attachSurface(holder.surface)
                MPVLib.setOptionString("force-window", "yes")
                MPVLib.setOptionString("vo", "gpu")
            }

            /**
             * This is called immediately after any structural changes (format or
             * size) have been made to the surface.  You should at this point update
             * the imagery in the surface.  This method is always called at least
             * once, after [.surfaceCreated].
             *
             * @param holder The SurfaceHolder whose surface has changed.
             * @param format The new [android.graphics.PixelFormat] of the surface.
             * @param width The new width of the surface.
             * @param height The new height of the surface.
             */
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                MPVLib.setPropertyString("android-surface-size", "${width}x$height")
            }

            /**
             * This is called immediately before a surface is being destroyed. After
             * returning from this call, you should no longer try to access this
             * surface.  If you have a rendering thread that directly accesses
             * the surface, you must ensure that thread is no longer touching the
             * Surface before returning from this function.
             *
             * @param holder The SurfaceHolder whose surface is being destroyed.
             */
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                MPVLib.setOptionString("vo", "null")
                MPVLib.setOptionString("force-window", "no")
                MPVLib.detachSurface()
            }
        }

        @Parcelize
        data class Track(
            val id: Int,
            @TrackType val type: String,
            val mimeType: String = when (type) {
                TrackType.VIDEO -> MimeTypes.BASE_TYPE_VIDEO
                TrackType.AUDIO -> MimeTypes.BASE_TYPE_AUDIO
                TrackType.SUBTITLE -> MimeTypes.BASE_TYPE_TEXT
                else -> ""
            },
            val title: String,
            val lang: String,
            val external: Boolean,
            val selected: Boolean,
            val externalFilename: String?,
            val ffIndex: Int,
            val codec: String,
            val width: Int?,
            val height: Int?
        ) : Parcelable {
            fun toFormat() : Format {
                return Format.Builder()
                    .setId(id)
                    .setContainerMimeType("$mimeType/$codec")
                    .setSampleMimeType("$mimeType/$codec")
                    .setCodecs(codec)
                    .setWidth(width ?: Format.NO_VALUE)
                    .setHeight(height ?: Format.NO_VALUE)
                    .build()
            }
            companion object {
                fun fromJSON(json: JSONObject): Track {
                    return Track(
                        id = json.optInt("id"),
                        type = json.optString("type"),
                        title = json.optString("title"),
                        lang = json.optString("lang"),
                        external = json.getBoolean("external"),
                        selected = json.getBoolean("selected"),
                        externalFilename = json.optString("external-filename"),
                        ffIndex = json.optInt("ff-index"),
                        codec = json.optString("codec"),
                        width = json.optInt("demux-w").takeIf { it > 0 },
                        height = json.optInt("demux-h").takeIf { it > 0 }
                    )
                }
            }
        }

        private fun getMPVTracks(trackList: String) : Triple<List<Track> ,TrackGroupArray, TrackSelectionArray> {
            val tracks = mutableListOf<Track>()
            var trackGroupArray = TrackGroupArray.EMPTY
            var trackSelectionArray = TrackSelectionArray()

            val trackListVideo = mutableListOf<Format>()
            val trackListAudio = mutableListOf<Format>()
            val trackListText = mutableListOf<Format>()
            var indexCurrentVideo: Int = C.INDEX_UNSET
            var indexCurrentAudio: Int = C.INDEX_UNSET
            var indexCurrentText: Int = C.INDEX_UNSET
            try {
                val currentTrackList = JSONArray(trackList)
                for (index in 0 until currentTrackList.length()) {
                    val currentTrack = Track.fromJSON(currentTrackList.getJSONObject(index))
                    val currentFormat = currentTrack.toFormat()
                    when (currentTrack.type) {
                        TrackType.VIDEO -> {
                            tracks.add(currentTrack)
                            trackListVideo.add(currentFormat)
                            if (currentTrack.selected) {
                                indexCurrentVideo = trackListVideo.indexOf(currentFormat)
                            }
                        }
                        TrackType.AUDIO -> {
                            tracks.add(currentTrack)
                            trackListAudio.add(currentFormat)
                            if (currentTrack.selected) {
                                indexCurrentAudio = trackListAudio.indexOf(currentFormat)
                            }
                        }
                        TrackType.SUBTITLE -> {
                            tracks.add(currentTrack)
                            trackListText.add(currentFormat)
                            if (currentTrack.selected) {
                                indexCurrentText = trackListText.indexOf(currentFormat)
                            }
                        }
                        else -> continue
                    }
                }
                val trackGroups = mutableListOf<TrackGroup>()
                val trackSelections = mutableListOf<TrackSelection>()
                if (trackListVideo.isNotEmpty()) {
                    with(TrackGroup(*trackListVideo.toTypedArray())) {
                        trackGroups.add(this)
                        trackSelections.add(CurrentTrackSelection(this, indexCurrentVideo))
                    }
                }
                if (trackListAudio.isNotEmpty()) {
                    with(TrackGroup(*trackListAudio.toTypedArray())) {
                        trackGroups.add(this)
                        trackSelections.add(CurrentTrackSelection(this, indexCurrentAudio))
                    }
                }
                if (trackListText.isNotEmpty()) {
                    with(TrackGroup(*trackListText.toTypedArray())) {
                        trackGroups.add(this)
                        trackSelections.add(CurrentTrackSelection(this, indexCurrentText))
                    }
                }
                if (trackGroups.isNotEmpty()) {
                    trackGroupArray = TrackGroupArray(*trackGroups.toTypedArray())
                    trackSelectionArray = TrackSelectionArray(*trackSelections.toTypedArray())
                }
            } catch (e: JSONException) {}
            return Triple(tracks, trackGroupArray, trackSelectionArray)
        }

        const val PLAYER_NAME = "MPV Player"

        /**
         * Merges multiple [subtitleSources] into a single [videoSource]
         */
        fun mergeMediaSources(
            videoSource: MediaSource,
            subtitleSources: Array<MediaSource>,
            dataSource: DataSource.Factory
        ): MediaSource {
            return when {
                subtitleSources.isEmpty() -> videoSource
                else -> {
                    val subtitles = mutableListOf<MediaItem.Subtitle>()
                    subtitleSources.forEach { subtitleSource ->
                        subtitleSource.mediaItem.playbackProperties?.subtitles?.forEach { subtitle ->
                            subtitles.add(subtitle)
                        }
                    }
                    ProgressiveMediaSource.Factory(dataSource)
                        .createMediaSource(
                            videoSource.mediaItem.buildUpon()
                                .setSubtitles(subtitles).build()
                        )
                }
            }
        }
    }
}
