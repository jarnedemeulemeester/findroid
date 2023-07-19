package dev.jdtech.jellyfin.mpv

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.core.content.getSystemService
import androidx.media3.common.AudioAttributes
import androidx.media3.common.BasePlayer
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.FlagSet
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.Commands
import androidx.media3.common.Timeline
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Clock
import androidx.media3.common.util.ListenerSet
import androidx.media3.common.util.Size
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlaybackException
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.mpv.MPVLib
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArraySet

@Suppress("SpellCheckingInspection")
class MPVPlayer(
    context: Context,
    private val requestAudioFocus: Boolean,
    private val appPreferences: AppPreferences,
) : BasePlayer(), MPVLib.EventObserver, AudioManager.OnAudioFocusChangeListener {

    private val audioManager: AudioManager by lazy { context.getSystemService()!! }
    private var audioFocusCallback: () -> Unit = {}
    private var currentIndex = 0
    private lateinit var audioFocusRequest: AudioFocusRequest
    private val handler = Handler(context.mainLooper)

    init {
        require(context is Application)
        val mpvDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "mpv")
        Timber.i("mpv config dir: $mpvDir")
        if (!mpvDir.exists()) mpvDir.mkdirs()
        arrayOf("mpv.conf", "subfont.ttf").forEach { fileName ->
            val file = File(mpvDir, fileName)
            if (file.exists()) return@forEach
            context.assets.open(fileName, AssetManager.ACCESS_STREAMING)
                .copyTo(FileOutputStream(file))
        }
        MPVLib.create(context)

        // General
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", mpvDir.path)
        MPVLib.setOptionString("vo", appPreferences.playerMpvVo)
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("gpu-api", appPreferences.playerMpvGpuApi)
        MPVLib.setOptionString("ao", appPreferences.playerMpvAo)

        // Hardware video decoding
        MPVLib.setOptionString("hwdec", appPreferences.playerMpvHwdec)
        MPVLib.setOptionString("hwdec-codecs", appPreferences.playerMpvHwdecCodecs.joinToString(separator = ","))

        // TLS
        MPVLib.setOptionString("tls-verify", "no")

        // Cache
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-pause-initial", "yes")
        MPVLib.setOptionString("demuxer-max-bytes", "32MiB")
        MPVLib.setOptionString("demuxer-max-back-bytes", "32MiB")

        // Subs
        MPVLib.setOptionString("sub-scale-with-window", "yes")
        MPVLib.setOptionString("sub-use-margins", "no")

        // Language
        MPVLib.setOptionString("alang", appPreferences.preferredAudioLanguage)
        MPVLib.setOptionString("slang", appPreferences.preferredSubtitleLanguage)

        // Other options
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("keep-open", "always")
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("sub-font-provider", "none")
        MPVLib.setOptionString("ytdl", "no")
        // DR is known to ruin performance at least on Exynos devices, see mpv-android#508
        MPVLib.setOptionString("vd-lavc-dr", "no")

        MPVLib.init()

        companionPrefs = appPreferences

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
            Property("speed", MPVLib.MPV_FORMAT_DOUBLE),
        ).forEach { (name, format) ->
            MPVLib.observeProperty(name, format)
        }

        if (requestAudioFocus) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(this)
                .build()
            val res = audioManager.requestAudioFocus(audioFocusRequest)
            if (res != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MPVLib.setPropertyBoolean("pause", true)
            }
        }
    }

    // Listeners and notification.
    private val listeners: ListenerSet<Player.Listener> = ListenerSet(
        context.mainLooper,
        Clock.DEFAULT,
    ) { listener: Player.Listener, flags: FlagSet ->
        listener.onEvents(this, Player.Events(flags))
    }
    private val videoListeners =
        CopyOnWriteArraySet<Player.Listener>()

    // Internal state.
    private var internalMediaItems = mutableListOf<MediaItem>()

    @Player.State
    private var playbackState: Int = Player.STATE_IDLE
    private var currentPlayWhenReady: Boolean = false

    @Player.RepeatMode
    private val repeatMode: Int = REPEAT_MODE_OFF
    private var tracks: Tracks = Tracks.EMPTY
    private var playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT

    // MPV Custom
    private var isPlayerReady: Boolean = false
    private var isSeekable: Boolean = false
    private var currentPositionMs: Long? = null
    private var currentDurationMs: Long? = null
    private var currentCacheDurationMs: Long? = null
    var currentMpvTracks: List<Track> = emptyList()
    private var initialCommands = mutableListOf<Array<String>>()
    private var initialSeekTo: Long = 0L
    private var trackSelectionParameters: TrackSelectionParameters = TrackSelectionParameters.Builder(context).build()

    // mpv events
    override fun eventProperty(property: String) {
        // Nothing to do...
    }

    override fun eventProperty(property: String, value: String) {
        handler.post {
            when (property) {
                "track-list" -> {
                    val (mpvTracks, newTracks) = getMPVTracks(value)
                    mpvTracks.forEach { Timber.i("${it.ffIndex} ${it.type} ${it.codec}") }
                    currentMpvTracks = mpvTracks
                    if (isPlayerReady) {
                        if (newTracks != tracks) {
                            tracks = newTracks
//                            listeners.sendEvent(Player.EVENT_TRACKS_CHANGED) { listener ->
//                                listener.onTracksChanged(currentTracks)
//                            }
                        }
                    } else {
                        tracks = newTracks
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
                        if (currentIndex < (internalMediaItems.size - 1)) {
                            currentIndex += 1
                            prepareMediaItem(currentIndex)
                            play()
                        } else {
                            setPlayerStateAndNotifyIfChanged(
                                playWhenReady = false,
                                playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM,
                                playbackState = Player.STATE_ENDED,
                            )
                            resetInternalState()
                        }
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
                            listener.onTimelineChanged(
                                timeline,
                                Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
                            )
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
                            listener.onTimelineChanged(
                                timeline,
                                Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
                            )
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
                    listeners.sendEvent(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED) { listener ->
                        listener.onPlaybackParametersChanged(getPlaybackParameters())
                    }
                }
            }
        }
    }

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
                            listener.onTracksChanged(currentTracks)
                        }
                        seekTo(C.TIME_UNSET)
                        if (playWhenReady) {
                            Timber.d("Starting playback...")
                            MPVLib.setPropertyBoolean("pause", false)
                        }
                        for (videoListener in videoListeners) {
                            videoListener.onRenderedFirstFrame()
                        }
                    } else {
                        setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_READY)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun setPlayerStateAndNotifyIfChanged(
        playWhenReady: Boolean = getPlayWhenReady(),
        @Player.PlayWhenReadyChangeReason playWhenReadyChangeReason: Int = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        @Player.State playbackState: Int = getPlaybackState(),
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
            listeners.queueEvent(C.INDEX_UNSET) { listener ->
                listener.onPlaybackStateChanged(playbackState)
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
     * @param id Id to select or [C.INDEX_UNSET] to disable [TrackType]
     * @return true if the track is or was already selected
     */
    fun selectTrack(
        trackType: TrackType,
        id: Int,
    ) {
        if (id != C.INDEX_UNSET) {
            MPVLib.setPropertyInt(trackType.type, id)
        } else {
            MPVLib.setPropertyString(trackType.type, "no")
        }
    }

    // Timeline wrapper
    private val timeline: Timeline = object : Timeline() {
        /**
         * Returns the number of windows in the timeline.
         */
        override fun getWindowCount(): Int {
            return internalMediaItems.size
        }

        /**
         * Populates a [androidx.media3.common.Timeline.Window] with data for the window at the specified index.
         *
         * @param windowIndex The index of the window.
         * @param window The [androidx.media3.common.Timeline.Window] to populate. Must not be null.
         * @param defaultPositionProjectionUs A duration into the future that the populated window's
         * default start position should be projected.
         * @return The populated [androidx.media3.common.Timeline.Window], for convenience.
         */
        override fun getWindow(
            windowIndex: Int,
            window: Window,
            defaultPositionProjectionUs: Long,
        ): Window {
            val currentMediaItem =
                internalMediaItems.getOrNull(windowIndex) ?: MediaItem.Builder().build()
            return window.set(
                /* uid = */
                windowIndex,
                /* mediaItem = */
                currentMediaItem,
                /* manifest = */
                null,
                /* presentationStartTimeMs = */
                C.TIME_UNSET,
                /* windowStartTimeMs = */
                C.TIME_UNSET,
                /* elapsedRealtimeEpochOffsetMs = */
                C.TIME_UNSET,
                /* isSeekable = */
                isSeekable,
                /* isDynamic = */
                !isSeekable,
                /* liveConfiguration = */
                currentMediaItem.liveConfiguration,
                /* defaultPositionUs = */
                C.TIME_UNSET,
                /* durationUs = */
                Util.msToUs(currentDurationMs ?: C.TIME_UNSET),
                /* firstPeriodIndex = */
                windowIndex,
                /* lastPeriodIndex = */
                windowIndex,
                /* positionInFirstPeriodUs = */
                C.TIME_UNSET,
            )
        }

        /**
         * Returns the number of periods in the timeline.
         */
        override fun getPeriodCount(): Int {
            return internalMediaItems.size
        }

        /**
         * Populates a [androidx.media3.common.Timeline.Period] with data for the period at the specified index.
         *
         * @param periodIndex The index of the period.
         * @param period The [androidx.media3.common.Timeline.Period] to populate. Must not be null.
         * @param setIds Whether [androidx.media3.common.Timeline.Period.id] and [androidx.media3.common.Timeline.Period.uid] should be populated. If false,
         * the fields will be set to null. The caller should pass false for efficiency reasons unless
         * the fields are required.
         * @return The populated [androidx.media3.common.Timeline.Period], for convenience.
         */
        override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
            return period.set(
                /* id = */
                periodIndex,
                /* uid = */
                periodIndex,
                /* windowIndex = */
                periodIndex,
                /* durationUs = */
                Util.msToUs(currentDurationMs ?: C.TIME_UNSET),
                /* positionInWindowUs = */
                0,
            )
        }

        /**
         * Returns the index of the period identified by its unique [Timeline.Period.uid], or [ ][C.INDEX_UNSET] if the period is not in the timeline.
         *
         * @param uid A unique identifier for a period.
         * @return The index of the period, or [C.INDEX_UNSET] if the period was not found.
         */
        override fun getIndexOfPeriod(uid: Any): Int {
            return uid as Int
        }

        /**
         * Returns the unique id of the period identified by its index in the timeline.
         *
         * @param periodIndex The index of the period.
         * @return The unique id of the period.
         */
        override fun getUidOfPeriod(periodIndex: Int): Any {
            return periodIndex
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
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> {
                val oldAudioFocusCallback = audioFocusCallback
                val wasPlaying = isPlaying
                MPVLib.setPropertyBoolean("pause", true)
                setPlayerStateAndNotifyIfChanged(
                    playWhenReady = false,
                    playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                )
                audioFocusCallback = {
                    oldAudioFocusCallback()
                    if (wasPlaying) MPVLib.setPropertyBoolean("pause", false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                MPVLib.command(arrayOf("multiply", "volume", "$AUDIO_FOCUS_DUCKING"))
                audioFocusCallback = {
                    MPVLib.command(arrayOf("multiply", "volume", "${1f / AUDIO_FOCUS_DUCKING}"))
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
     * Registers a listener to receive all events from the player.
     *
     * @param listener The listener to register.
     */
    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
        videoListeners.add(listener)
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
     * @param startWindowIndex The window index to start playback from. If [C.INDEX_UNSET] is
     * passed, the current position is not reset.
     * @param startPositionMs The position in milliseconds to start playback from. If [     ][C.TIME_UNSET] is passed, the default position of the given window is used. In any case, if
     * `startWindowIndex` is set to [C.INDEX_UNSET], this parameter is ignored and the
     * position is not reset at all.
     * @throws androidx.media3.common.IllegalSeekPositionException If the provided `startWindowIndex` is not within the
     * bounds of the list of media items.
     */
    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startWindowIndex: Int,
        startPositionMs: Long,
    ) {
        internalMediaItems = mediaItems
        currentIndex = startWindowIndex
        initialSeekTo = startPositionMs / 1000
    }

    /**
     * Adds a list of media items at the given index of the playlist.
     *
     * @param index The index at which to add the media items. If the index is larger than the size of
     * the playlist, the media items are added to the end of the playlist.
     * @param mediaItems The [MediaItems][MediaItem] to add.
     */
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        internalMediaItems.addAll(mediaItems)
        mediaItems.forEach { mediaItem ->
            MPVLib.command(
                arrayOf(
                    "loadfile",
                    "${mediaItem.localConfiguration?.uri}",
                    "append",
                ),
            )
        }
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

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: MutableList<MediaItem>,
    ) {
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
     * Returns the player's currently available [Commands].
     *
     *
     * The returned [Commands] are not updated when available commands change. Use [ ][androidx.media3.common.Player.Listener.onAvailableCommandsChanged] to get an update when the available commands
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
     * @return The currently available [Commands].
     * @see androidx.media3.common.Player.Listener.onAvailableCommandsChanged
     */
    override fun getAvailableCommands(): Commands {
        return Commands.Builder()
            .addAll(permanentAvailableCommands)
            .addIf(COMMAND_SEEK_TO_DEFAULT_POSITION, !isPlayingAd)
            .addIf(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, isCurrentMediaItemSeekable && !isPlayingAd)
            .addIf(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, hasPreviousMediaItem() && !isPlayingAd)
            .addIf(
                COMMAND_SEEK_TO_PREVIOUS,
                !currentTimeline.isEmpty &&
                    (hasPreviousMediaItem() || !isCurrentMediaItemLive || isCurrentMediaItemSeekable) &&
                    !isPlayingAd,
            )
            .addIf(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, hasNextMediaItem() && !isPlayingAd)
            .addIf(
                COMMAND_SEEK_TO_NEXT,
                !currentTimeline.isEmpty &&
                    (hasNextMediaItem() || (isCurrentMediaItemLive && isCurrentMediaItemDynamic)) &&
                    !isPlayingAd,
            )
            .addIf(COMMAND_SEEK_TO_MEDIA_ITEM, !isPlayingAd)
            .addIf(COMMAND_SEEK_BACK, isCurrentMediaItemSeekable && !isPlayingAd)
            .addIf(COMMAND_SEEK_FORWARD, isCurrentMediaItemSeekable && !isPlayingAd)
            .build()
    }

    private fun resetInternalState() {
        isPlayerReady = false
        isSeekable = false
        playbackState = Player.STATE_IDLE
        currentPlayWhenReady = false
        currentPositionMs = null
        currentDurationMs = null
        currentCacheDurationMs = null
        tracks = Tracks.EMPTY
        playbackParameters = PlaybackParameters.DEFAULT
        initialCommands.clear()
        // initialSeekTo = 0L
    }

    /** Prepares the player.  */
    override fun prepare() {
        internalMediaItems.forEach { mediaItem ->
            MPVLib.command(
                arrayOf(
                    "loadfile",
                    "${mediaItem.localConfiguration?.uri}",
                    "append",
                ),
            )
        }
        prepareMediaItem(currentIndex)
    }

    /**
     * Returns the current [playback state][androidx.media3.common.Player.State] of the player.
     *
     * @return The current [playback state][androidx.media3.common.Player.State].
     * @see androidx.media3.common.Player.Listener.onPlaybackStateChanged
     */
    override fun getPlaybackState(): Int {
        return playbackState
    }

    /**
     * Returns the reason why playback is suppressed even though [.getPlayWhenReady] is `true`, or [.PLAYBACK_SUPPRESSION_REASON_NONE] if playback is not suppressed.
     *
     * @return The current [playback suppression reason][androidx.media3.common.Player.PlaybackSuppressionReason].
     * @see androidx.media3.common.Player.Listener.onPlaybackSuppressionReasonChanged
     */
    override fun getPlaybackSuppressionReason(): Int {
        return PLAYBACK_SUPPRESSION_REASON_NONE
    }

    /**
     * Returns the error that caused playback to fail. This is the same error that will have been
     * reported via [androidx.media3.common.Player.Listener.onPlayerError] at the time of failure. It
     * can be queried using this method until the player is re-prepared.
     *
     *
     * Note that this method will always return `null` if [.getPlaybackState] is not
     * [.STATE_IDLE].
     *
     * @return The error, or `null`.
     * @see androidx.media3.common.Player.Listener.onPlayerError
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
                playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
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
     * @see androidx.media3.common.Player.Listener.onPlayWhenReadyChanged
     */
    override fun getPlayWhenReady(): Boolean {
        return currentPlayWhenReady
    }

    /**
     * Sets the [androidx.media3.common.Player.RepeatMode] to be used for playback.
     *
     * @param repeatMode The repeat mode.
     */
    override fun setRepeatMode(repeatMode: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the current [androidx.media3.common.Player.RepeatMode] used for playback.
     *
     * @return The current repeat mode.
     * @see androidx.media3.common.Player.Listener.onRepeatModeChanged
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
     * @see androidx.media3.common.Player.Listener.onShuffleModeEnabledChanged
     */
    override fun getShuffleModeEnabled(): Boolean {
        return false
    }

    /**
     * Whether the player is currently loading the source.
     *
     * @return Whether the player is currently loading the source.
     * @see androidx.media3.common.Player.Listener.onIsLoadingChanged
     */
    override fun isLoading(): Boolean {
        return false
    }

    /**
     * Seeks to a position specified in milliseconds in the specified window.
     *
     * @param mediaItemIndex The index of the window.
     * @param positionMs The seek position in the specified window, or [C.TIME_UNSET] to seek to
     * the window's default position.
     * @param seekCommand The {@link Player.Command} used to trigger the seek.
     * @param isRepeatingCurrentItem Whether this seeks repeats the current item.
     * @throws androidx.media3.common.IllegalSeekPositionException If the player has a non-empty timeline and the provided
     * `windowIndex` is not within the bounds of the current timeline.
     */
    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
        @Player.Command seekCommand: Int,
        isRepeatingCurrentItem: Boolean,
    ) {
        if (mediaItemIndex == currentMediaItemIndex) {
            val seekTo =
                if (positionMs != C.TIME_UNSET) positionMs / C.MILLIS_PER_SECOND else initialSeekTo
            initialSeekTo = if (isPlayerReady) {
                MPVLib.command(arrayOf("seek", "$seekTo", "absolute"))
                0L
            } else {
                seekTo
            }
        } else {
            prepareMediaItem(mediaItemIndex)
            play()
        }
    }

    private fun prepareMediaItem(index: Int) {
        internalMediaItems.getOrNull(index)?.let { mediaItem ->
            resetInternalState()
            mediaItem.localConfiguration?.subtitleConfigurations?.forEach { subtitle ->
                initialCommands.add(
                    arrayOf(
                        /* command= */
                        "sub-add",
                        /* url= */
                        "${subtitle.uri}",
                        /* flags= */
                        "auto",
                        /* title= */
                        "${subtitle.label}",
                        /* lang= */
                        "${subtitle.language}",
                    ),
                )
            }
            currentIndex = index
            MPVLib.command(arrayOf("playlist-play-index", "$index"))
            MPVLib.setPropertyBoolean("pause", true)
            listeners.sendEvent(Player.EVENT_TIMELINE_CHANGED) { listener ->
                listener.onTimelineChanged(timeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            }
            listeners.sendEvent(Player.EVENT_MEDIA_ITEM_TRANSITION) { listener ->
                listener.onMediaItemTransition(
                    mediaItem,
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
                )
            }
            setPlayerStateAndNotifyIfChanged(playbackState = Player.STATE_BUFFERING)
        }
    }

    override fun getSeekBackIncrement(): Long {
        return appPreferences.playerSeekBackIncrement
    }

    override fun getSeekForwardIncrement(): Long {
        return appPreferences.playerSeekForwardIncrement
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS
    }

    /**
     * Attempts to set the playback parameters. Passing [PlaybackParameters.DEFAULT] resets the
     * player to the default, which means there is no speed or pitch adjustment.
     *
     *
     * Playback parameters changes may cause the player to buffer. [ ][androidx.media3.common.Player.Listener.onPlaybackParametersChanged] will be called whenever the currently
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
     * @see androidx.media3.common.Player.Listener.onPlaybackParametersChanged
     */
    override fun getPlaybackParameters(): PlaybackParameters {
        return playbackParameters
    }

    override fun stop() {
        MPVLib.command(arrayOf("stop", "keep-playlist"))
    }

    /**
     * Releases the player. This method must be called when the player is no longer required. The
     * player must not be used after calling this method.
     */
    override fun release() {
        if (requestAudioFocus) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        resetInternalState()
        MPVLib.removeObserver(this)
        MPVLib.destroy()
        currentIndex = 0
    }

    override fun getCurrentTracks(): Tracks {
        return tracks
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return trackSelectionParameters
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        trackSelectionParameters = parameters
    }

    /**
     * Returns the current combined [MediaMetadata], or [MediaMetadata.EMPTY] if not
     * supported.
     *
     *
     * This [MediaMetadata] is a combination of the [MediaItem.mediaMetadata] and the
     * static and dynamic metadata sourced from [androidx.media3.common.Player.Listener.onMediaMetadataChanged] and
     * [androidx.media3.exoplayer.metadata.MetadataOutput.onMetadata].
     */
    override fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.EMPTY
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        return MediaMetadata.EMPTY
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the current [Timeline]. Never null, but may be empty.
     *
     * @see androidx.media3.common.Player.Listener.onTimelineChanged
     */
    override fun getCurrentTimeline(): Timeline {
        return timeline
    }

    /** Returns the index of the period currently being played.  */
    override fun getCurrentPeriodIndex(): Int {
        return currentMediaItemIndex
    }

    override fun getCurrentMediaItemIndex(): Int {
        return currentIndex
    }

    /**
     * Returns the duration of the current content window or ad in milliseconds, or [ ][C.TIME_UNSET] if the duration is not known.
     */
    override fun getDuration(): Long {
        return timeline.getWindow(currentMediaItemIndex, window).durationMs
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
     * currently being played. Returns [C.INDEX_UNSET] otherwise.
     */
    override fun getCurrentAdGroupIndex(): Int {
        return C.INDEX_UNSET
    }

    /**
     * If [.isPlayingAd] returns true, returns the index of the ad in its ad group. Returns
     * [C.INDEX_UNSET] otherwise.
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
        return MPVLib.getPropertyInt("volume") / 100F
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
     * @see androidx.media3.common.Player.Listener.onVideoSizeChanged
     */
    override fun getVideoSize(): VideoSize {
        return VideoSize.UNKNOWN
    }

    override fun getSurfaceSize(): Size {
        val mpvSize = MPVLib.getPropertyString("android-surface-size").split("x")
        return try {
            Size(mpvSize[0].toInt(), mpvSize[1].toInt())
        } catch (_: IndexOutOfBoundsException) {
            Size.UNKNOWN
        }
    }

    /** Returns the current [CueGroup]. This list may be empty.  */
    override fun getCurrentCues(): CueGroup {
        return CueGroup(emptyList(), 0)
    }

    /** Gets the device information.  */
    override fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL)
            .setMaxVolume(0)
            .setMaxVolume(100)
            .build()
    }

    /**
     * Gets the current volume of the device.
     *
     *
     * For devices with [local playback][DeviceInfo.PLAYBACK_TYPE_LOCAL], the volume returned
     * by this method varies according to the current [stream type][C.StreamType]. The stream
     * type is determined by [AudioAttributes.usage] which can be converted to stream type with
     * [Util.getStreamTypeForAudioUsage].
     *
     *
     * For devices with [remote playback][DeviceInfo.PLAYBACK_TYPE_REMOTE], the volume of the
     * remote device is returned.
     */
    override fun getDeviceVolume(): Int {
        return MPVLib.getPropertyInt("volume")
    }

    /** Gets whether the device is muted or not.  */
    override fun isDeviceMuted(): Boolean {
        return MPVLib.getPropertyBoolean("mute")
    }

    /**
     * Sets the volume of the device.
     *
     * @param volume The volume to set.
     */
    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {
        throw IllegalArgumentException("You should use global volume controls. Check out AUDIO_SERVICE.")
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        MPVLib.setPropertyInt("volume", volume)
    }

    /** Increases the volume of the device.  */
    @Deprecated("Deprecated in Java")
    override fun increaseDeviceVolume() {
        throw IllegalArgumentException("You should use global volume controls. Check out AUDIO_SERVICE.")
    }

    override fun increaseDeviceVolume(flags: Int) {
        throw IllegalArgumentException("You should use global volume controls. Check out AUDIO_SERVICE.")
    }

    /** Decreases the volume of the device.  */
    @Deprecated("Deprecated in Java")
    override fun decreaseDeviceVolume() {
        throw IllegalArgumentException("You should use global volume controls. Check out AUDIO_SERVICE.")
    }

    override fun decreaseDeviceVolume(flags: Int) {
        throw IllegalArgumentException("You should use global volume controls. Check out AUDIO_SERVICE.")
    }

    /** Sets the mute state of the device.  */
    @Deprecated("Deprecated in Java")
    override fun setDeviceMuted(muted: Boolean) {
        throw IllegalArgumentException("You should use global volume controls. Check out AUDIO_SERVICE.")
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        return MPVLib.setPropertyBoolean("mute", muted)
    }

    fun updateZoomMode(enabled: Boolean) {
        if (enabled) {
            MPVLib.setOptionString("panscan", "1")
            MPVLib.setOptionString("sub-use-margins", "yes")
            MPVLib.setOptionString("sub-ass-force-margins", "yes")
        } else {
            MPVLib.setOptionString("panscan", "0")
            MPVLib.setOptionString("sub-use-margins", "no")
            MPVLib.setOptionString("sub-ass-force-margins", "no")
        }
    }

    companion object {
        /**
         * Fraction to which audio volume is ducked on loss of audio focus
         */
        private const val AUDIO_FOCUS_DUCKING = 0.5f

        private val permanentAvailableCommands: Commands = Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_SET_SPEED_AND_PITCH,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                COMMAND_GET_METADATA,
                COMMAND_CHANGE_MEDIA_ITEMS,
                COMMAND_SET_VIDEO_SURFACE,
            )
            .build()

        private lateinit var companionPrefs: AppPreferences

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
                MPVLib.setOptionString("vo", companionPrefs.playerMpvVo)
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
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
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
            val type: TrackType,
            val mimeType: String = when (type) {
                TrackType.VIDEO -> MimeTypes.BASE_TYPE_VIDEO
                TrackType.AUDIO -> MimeTypes.BASE_TYPE_AUDIO
                TrackType.SUBTITLE -> MimeTypes.BASE_TYPE_TEXT
            },
            val title: String,
            val lang: String,
            val external: Boolean,
            val selected: Boolean,
            val externalFilename: String?,
            val ffIndex: Int,
            val codec: String,
            val width: Int?,
            val height: Int?,
        ) : Parcelable {
            fun toFormat(): Format {
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
                        type = TrackType.values().first { it.type == json.optString("type") },
                        title = json.optString("title"),
                        lang = json.optString("lang"),
                        external = json.getBoolean("external"),
                        selected = json.getBoolean("selected"),
                        externalFilename = json.optString("external-filename"),
                        ffIndex = json.optInt("ff-index"),
                        codec = json.optString("codec"),
                        width = json.optInt("demux-w").takeIf { it > 0 },
                        height = json.optInt("demux-h").takeIf { it > 0 },
                    )
                }
            }
        }

        private fun getMPVTracks(trackList: String): Pair<List<Track>, Tracks> {
            val mpvTracks = mutableListOf<Track>()
            var tracks = Tracks.EMPTY
            val trackGroups = mutableListOf<Tracks.Group>()

            val trackListVideo = mutableListOf<Format>()
            val trackListAudio = mutableListOf<Format>()
            val trackListText = mutableListOf<Format>()
            var indexCurrentVideo: Int = C.INDEX_UNSET
            var indexCurrentAudio: Int = C.INDEX_UNSET
            var indexCurrentText: Int = C.INDEX_UNSET
            try {
                val emptyTrack = Track(
                    id = -1,
                    type = TrackType.SUBTITLE,
                    mimeType = MimeTypes.BASE_TYPE_TEXT,
                    title = "None",
                    lang = "",
                    external = false,
                    selected = false,
                    externalFilename = null,
                    ffIndex = -1,
                    codec = "",
                    width = null,
                    height = null,
                )
                mpvTracks.add(emptyTrack)
                trackListText.add(emptyTrack.toFormat())
                val currentTrackList = JSONArray(trackList)
                for (index in 0 until currentTrackList.length()) {
                    val currentTrack = Track.fromJSON(currentTrackList.getJSONObject(index))
                    val currentFormat = currentTrack.toFormat()
                    when (currentTrack.type) {
                        TrackType.VIDEO -> {
                            mpvTracks.add(currentTrack)
                            trackListVideo.add(currentFormat)
                            if (currentTrack.selected) {
                                indexCurrentVideo = trackListVideo.indexOf(currentFormat)
                            }
                        }
                        TrackType.AUDIO -> {
                            mpvTracks.add(currentTrack)
                            trackListAudio.add(currentFormat)
                            if (currentTrack.selected) {
                                indexCurrentAudio = trackListAudio.indexOf(currentFormat)
                            }
                        }
                        TrackType.SUBTITLE -> {
                            mpvTracks.add(currentTrack)
                            trackListText.add(currentFormat)
                            if (currentTrack.selected) {
                                indexCurrentText = trackListText.indexOf(currentFormat)
                            }
                        }
                    }
                }
                if (trackListText.size == 1 && trackListText[0].id == emptyTrack.id.toString()) {
                    mpvTracks.remove(emptyTrack)
                    trackListText.removeFirst()
                }
                if (trackListVideo.isNotEmpty()) {
                    trackGroups.add(
                        with(TrackGroup(*trackListVideo.toTypedArray())) {
                            Tracks.Group(
                                this,
                                true,
                                IntArray(this.length) { C.FORMAT_HANDLED },
                                BooleanArray(this.length) { it == indexCurrentVideo },
                            )
                        },
                    )
                }
                if (trackListAudio.isNotEmpty()) {
                    trackGroups.add(
                        with(TrackGroup(*trackListAudio.toTypedArray())) {
                            Tracks.Group(
                                this,
                                true,
                                IntArray(this.length) { C.FORMAT_HANDLED },
                                BooleanArray(this.length) { it == indexCurrentAudio },
                            )
                        },
                    )
                }
                if (trackListText.isNotEmpty()) {
                    trackGroups.add(
                        with(TrackGroup(*trackListText.toTypedArray())) {
                            Tracks.Group(
                                this,
                                true,
                                IntArray(this.length) { C.FORMAT_HANDLED },
                                BooleanArray(this.length) { it == indexCurrentText },
                            )
                        },
                    )
                }
                if (trackGroups.isNotEmpty()) {
                    tracks = Tracks(trackGroups)
                }
            } catch (_: JSONException) {
            }
            return Pair(mpvTracks, tracks)
        }
    }
}
