package dev.jdtech.jellyfin.car

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class FindroidCarVideoScreen(
    carContext: CarContext,
    private val item: FindroidCarCatalogItem,
    private val jellyfinRepository: JellyfinRepository,
) : Screen(carContext), SurfaceCallback {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val hideControlsRunnable =
        Runnable {
            controlsVisible = false
            presentation?.setControlsVisible(false)
            invalidate()
        }
    private val progressRunnable =
        object : Runnable {
            override fun run() {
                postPlaybackProgress(isPaused = !isPlaying)
                mainHandler.postDelayed(this, PLAYBACK_PROGRESS_INTERVAL_MS)
            }
        }
    private var player: ExoPlayer? = null
    private var surfaceContainer: SurfaceContainer? = null
    private var visibleArea: Rect? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: FindroidCarVideoPresentation? = null
    private var isPlaying = true
    private var controlsVisible = false
    private var aspectMode = FindroidCarVideoAspectMode.AUTO

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    setSurfaceCallback()
                    Timber.i("FindroidCarVideoScreen surface callback registered for %s", item.title)
                }

                override fun onStop(owner: LifecycleOwner) {
                    mainHandler.removeCallbacks(hideControlsRunnable)
                    mainHandler.removeCallbacks(progressRunnable)
                    clearSurfaceCallback()
                    stopPlaybackReporting()
                    releasePlayer()
                    releasePresentation()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    mainHandler.removeCallbacks(hideControlsRunnable)
                    mainHandler.removeCallbacks(progressRunnable)
                    clearSurfaceCallback()
                    stopPlaybackReporting()
                    releasePlayer()
                    releasePresentation()
                    scope.cancel()
                }
            }
        )
    }

    override fun onGetTemplate(): Template {
        if (item.videoPath.isNullOrBlank() && item.streamUrl.isNullOrBlank()) {
            return MessageTemplate.Builder("Video stream is unavailable")
                .setTitle(item.title)
                .setHeaderAction(Action.BACK)
                .build()
        }

        if (!controlsVisible) {
            return navigationTemplate(hiddenActionStrip())
        }

        return navigationTemplate(playbackActionStrip())
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        this.surfaceContainer = surfaceContainer
        Timber.i(
            "FindroidCarVideoScreen surface available: %sx%s dpi=%s",
            surfaceContainer.width,
            surfaceContainer.height,
            surfaceContainer.dpi,
        )
        startPlayer(surfaceContainer)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Timber.i("FindroidCarVideoScreen surface destroyed")
        if (this.surfaceContainer === surfaceContainer) {
            this.surfaceContainer = null
        }
        clearSurfaceCallback()
        stopPlaybackReporting()
        releasePlayer()
        releasePresentation()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = Rect(visibleArea)
        Timber.i("FindroidCarVideoScreen visible area changed: %s", visibleArea)
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        Timber.i("FindroidCarVideoScreen stable area changed: %s", stableArea)
    }

    override fun onClick(x: Float, y: Float) {
        Timber.i("FindroidCarVideoScreen surface click at %s,%s", x, y)
        if (controlsVisible && handleAspectModeClick(x, y)) return
        if (handleSeekbarClick(x, y)) return
        if (handleSurfaceControlClick(x, y)) return
        showControlsTemporarily()
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        if (!controlsVisible) return
        if (kotlin.math.abs(distanceX) <= kotlin.math.abs(distanceY)) return
        val exoPlayer = player ?: return
        val duration = effectiveDurationMs(exoPlayer).takeIf { it > 0L } ?: return
        val trackWidth = seekbarTrackWidth().takeIf { it > 0f } ?: return
        val deltaMs = ((-distanceX / trackWidth) * duration).toLong()
        if (kotlin.math.abs(deltaMs) < MIN_SCROLL_SEEK_MS) return
        val target = (absolutePlaybackPositionMs(exoPlayer) + deltaMs).coerceIn(0L, duration)
        Timber.i(
            "FindroidCarVideoScreen seekbar scroll distanceX=%s distanceY=%s deltaMs=%s targetMs=%s durationMs=%s trackWidth=%s",
            distanceX,
            distanceY,
            deltaMs,
            target,
            duration,
            trackWidth,
        )
        seekToPosition(exoPlayer, source = "seekbar scroll", targetMs = target, durationMs = duration)
    }

    private fun startPlayer(surfaceContainer: SurfaceContainer) {
        releasePlayer()
        releasePresentation()
        val uri = item.playbackUri() ?: return
        val initialStartPositionMs = item.startPositionMs()
        val mediaItem = item.toCarMediaItem(uri)
        Timber.i(
            "FindroidCarVideoScreen starting ExoPlayer sourceType=%s pipeline=findroid-standard-media-item mediaId=%s uri=%s startPositionMs=%s runtimeMs=%s",
            item.playbackSourceKind(),
            mediaItem.mediaId,
            uri.toSanitizedLogString(),
            initialStartPositionMs,
            item.runtimeTicks.toMillis(),
        )
        Log.i(
            FINDROID_CAR_AA_LOG_TAG,
            "FindroidCarVideoScreen starting ExoPlayer sourceType=${item.playbackSourceKind()} pipeline=findroid-standard-media-item mediaId=${mediaItem.mediaId} startPositionMs=$initialStartPositionMs runtimeMs=${item.runtimeTicks.toMillis()}",
        )
        isPlaying = true
        val presentation = createPresentation(surfaceContainer) ?: return
        this.presentation = presentation
        presentation.setFallbackDuration(item.runtimeTicks.toMillis())
        presentation.setPlaybackTimeline(
            positionOffsetMs = 0L,
            durationMs = item.runtimeTicks.toMillis(),
        )
        presentation.show()
        val renderersFactory =
            DefaultRenderersFactory(carContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val trackSelector =
            DefaultTrackSelector(carContext).apply {
                setParameters(buildUponParameters().setTunnelingEnabled(true))
            }
        player =
            ExoPlayer.Builder(carContext, renderersFactory)
                .setAudioAttributes(movieAudioAttributes(), true)
                .setTrackSelector(trackSelector)
                .setSeekBackIncrementMs(SEEK_BACK_MS)
                .setSeekForwardIncrementMs(SEEK_FORWARD_MS)
                .setPauseAtEndOfMediaItems(true)
                .build()
                .also { exoPlayer ->
                    exoPlayer.addListener(playerListener())
                    exoPlayer.setSeekParameters(SeekParameters.EXACT)
                    exoPlayer.setHandleAudioBecomingNoisy(true)
                    presentation.setPlayer(exoPlayer)
                    exoPlayer.setMediaItems(
                        listOf(mediaItem),
                        0,
                        initialStartPositionMs.takeIf { it > 0L } ?: C.TIME_UNSET,
                    )
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
        postPlaybackStart()
        mainHandler.removeCallbacks(progressRunnable)
        mainHandler.postDelayed(progressRunnable, PLAYBACK_PROGRESS_INTERVAL_MS)
        hideControls()
    }

    private fun pausePlayback() {
        val exoPlayer = player ?: return
        if (!isPlaying) return
        exoPlayer.pause()
        isPlaying = false
        Timber.i("FindroidCarVideoScreen paused")
        postPlaybackProgress(isPaused = !isPlaying)
        showControlsTemporarily()
    }

    private fun resumePlayback() {
        val exoPlayer = player ?: return
        if (isPlaying) return
        exoPlayer.play()
        isPlaying = true
        Timber.i("FindroidCarVideoScreen resumed")
        postPlaybackProgress(isPaused = !isPlaying)
        showControlsTemporarily()
    }

    private fun seekBy(deltaMs: Long) {
        val exoPlayer = player ?: return
        val duration = effectiveDurationMs(exoPlayer)
        val maxPosition = if (duration > 0L) duration else Long.MAX_VALUE
        val target = (absolutePlaybackPositionMs(exoPlayer) + deltaMs).coerceIn(0L, maxPosition)
        Timber.i("FindroidCarVideoScreen seek by %sms to %sms", deltaMs, target)
        seekToPosition(
            exoPlayer,
            source = if (deltaMs >= 0) "button forward" else "button rewind",
            targetMs = target,
            durationMs = duration,
        )
    }

    private fun goBack() {
        Timber.i("FindroidCarVideoScreen back")
        clearSurfaceCallback()
        stopPlaybackReporting()
        releasePlayer()
        releasePresentation()
        carContext.getCarService(ScreenManager::class.java).pop()
    }

    private fun showControlsTemporarily() {
        controlsVisible = true
        presentation?.setControlsVisible(true)
        mainHandler.removeCallbacks(hideControlsRunnable)
        mainHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_DELAY_MS)
        invalidate()
    }

    private fun hideControls() {
        controlsVisible = false
        presentation?.setControlsVisible(false)
        mainHandler.removeCallbacks(hideControlsRunnable)
        invalidate()
    }

    private fun playbackActionStrip(): ActionStrip =
        ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(android.R.drawable.ic_media_rew))
                    .setOnClickListener { seekBy(-SEEK_BACK_MS) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(
                        carIcon(
                            if (isPlaying) android.R.drawable.ic_media_pause
                            else android.R.drawable.ic_media_play
                        )
                    )
                    .setOnClickListener {
                        if (isPlaying) pausePlayback() else resumePlayback()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(android.R.drawable.ic_media_ff))
                    .setOnClickListener { seekBy(SEEK_FORWARD_MS) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(android.R.drawable.ic_menu_revert))
                    .setOnClickListener { goBack() }
                    .build()
            )
            .build()

    private fun hiddenActionStrip(): ActionStrip =
        ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(CoreR.drawable.ic_transparent))
                    .setOnClickListener { showControlsTemporarily() }
                    .build()
            )
            .build()

    private fun panActionStrip(): ActionStrip =
        ActionStrip.Builder().addAction(Action.PAN).build()

    private fun navigationTemplate(actionStrip: ActionStrip): NavigationTemplate =
        NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setMapActionStrip(panActionStrip())
            .build()

    private fun setSurfaceCallback() {
        carContext
            .getCarService(AppManager::class.java)
            .setSurfaceCallback(this@FindroidCarVideoScreen)
    }

    private fun clearSurfaceCallback() {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
        surfaceContainer = null
        visibleArea = null
    }

    private fun releasePlayer() {
        mainHandler.removeCallbacks(hideControlsRunnable)
        mainHandler.removeCallbacks(progressRunnable)
        presentation?.setPlayer(null)
        player?.clearVideoSurface()
        player?.release()
        player = null
    }

    private fun handleSurfaceControlClick(x: Float, y: Float): Boolean {
        if (y > CONTROL_BUTTON_HIT_HEIGHT) return false
        val right =
            visibleArea?.right?.toFloat()
                ?: surfaceContainer?.width?.toFloat()
                ?: return false
        val action =
            listOf(
                    SurfaceControlAction(
                        "rewind",
                        right - REWIND_BUTTON_OFFSET_FROM_VISIBLE_RIGHT,
                    ) {
                        seekBy(-SEEK_BACK_MS)
                    },
                    SurfaceControlAction(
                        "playPause",
                        right - PLAY_PAUSE_BUTTON_OFFSET_FROM_VISIBLE_RIGHT,
                    ) {
                        if (isPlaying) pausePlayback() else resumePlayback()
                    },
                    SurfaceControlAction(
                        "forward",
                        right - FORWARD_BUTTON_OFFSET_FROM_VISIBLE_RIGHT,
                    ) {
                        seekBy(SEEK_FORWARD_MS)
                    },
                    SurfaceControlAction("back", right - BACK_BUTTON_OFFSET_FROM_VISIBLE_RIGHT) {
                        goBack()
                    },
                )
                .minByOrNull { kotlin.math.abs(x - it.centerX) }
                ?: return false
        val distance = kotlin.math.abs(x - action.centerX)
        if (distance > CONTROL_BUTTON_HIT_RADIUS) return false
        Timber.i(
            "FindroidCarVideoScreen surface control hit action=%s x=%s y=%s center=%s distance=%s right=%s",
            action.name,
            x,
            y,
            action.centerX,
            distance,
            right,
        )
        action.invoke()
        return true
    }

    private fun handleAspectModeClick(x: Float, y: Float): Boolean {
        val left =
            (visibleArea?.left?.toFloat() ?: 0f) + ASPECT_BUTTON_OFFSET_FROM_VISIBLE_LEFT
        val right = left + ASPECT_BUTTON_HIT_WIDTH
        if (y !in ASPECT_BUTTON_TOP..ASPECT_BUTTON_BOTTOM || x !in left..right) {
            return false
        }
        aspectMode = aspectMode.next()
        val appliedMode = presentation?.setAspectMode(aspectMode) ?: "UNKNOWN"
        Timber.i(
            "FindroidCarVideoScreen aspect mode changed requestedMode=%s appliedMode=%s x=%s y=%s hitLeft=%s hitRight=%s",
            aspectMode.name,
            appliedMode,
            x,
            y,
            left,
            right,
        )
        showControlsTemporarily()
        return true
    }

    private fun handleSeekbarClick(x: Float, y: Float): Boolean {
        val exoPlayer = player ?: return false
        val duration = effectiveDurationMs(exoPlayer).takeIf { it > 0L } ?: return false
        val visible = visibleArea
        val bottom =
            visible?.bottom?.toFloat()
                ?: surfaceContainer?.height?.toFloat()
                ?: return false
        val centerY = bottom - SEEK_BAR_CENTER_OFFSET_FROM_VISIBLE_BOTTOM
        if (y !in centerY - SEEK_BAR_HIT_VERTICAL_RADIUS..centerY + SEEK_BAR_HIT_VERTICAL_RADIUS) {
            return false
        }
        val trackLeft = seekbarTrackLeft()
        val trackRight = seekbarTrackRight()
        if (trackRight <= trackLeft) return false
        val fraction = ((x - trackLeft) / (trackRight - trackLeft)).coerceIn(0f, 1f)
        val target = (duration * fraction).toLong().coerceIn(0L, duration)
        Timber.i(
            "FindroidCarVideoScreen seekbar tap x=%s y=%s fraction=%s targetMs=%s durationMs=%s trackLeft=%s trackRight=%s visibleArea=%s controlsVisible=%s",
            x,
            y,
            fraction,
            target,
            duration,
            trackLeft,
            trackRight,
            visible,
            controlsVisible,
        )
        seekToPosition(exoPlayer, source = "seekbar tap", targetMs = target, durationMs = duration)
        return true
    }

    private fun seekToPosition(
        exoPlayer: ExoPlayer,
        source: String,
        targetMs: Long,
        durationMs: Long,
    ) {
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        val seekable = exoPlayer.isCurrentMediaItemSeekable
        Timber.i(
            "FindroidCarVideoScreen seek request source=%s strategy=native-media3-media-item sourceType=%s oldMs=%s oldPlayerMs=%s targetMs=%s durationMs=%s playerDurationMs=%s seekable=%s state=%s isPlaying=%s playWhenReady=%s bufferedMs=%s mediaItem=%s seekParameters=%s/%s",
            source,
            item.playbackSourceKind(),
            absolutePlaybackPositionMs(exoPlayer),
            exoPlayer.currentPosition,
            targetMs,
            durationMs,
            exoPlayer.duration,
            seekable,
            playbackStateName(exoPlayer.playbackState),
            exoPlayer.isPlaying,
            exoPlayer.playWhenReady,
            exoPlayer.bufferedPosition,
            exoPlayer.currentMediaItem?.localConfiguration?.uri?.toSanitizedLogString(),
            exoPlayer.seekParameters.toleranceBeforeUs,
            exoPlayer.seekParameters.toleranceAfterUs,
        )
        Log.i(
            FINDROID_CAR_AA_LOG_TAG,
            "FindroidCarVideoScreen seek request source=$source strategy=native-media3-media-item sourceType=${item.playbackSourceKind()} oldMs=${absolutePlaybackPositionMs(exoPlayer)} oldPlayerMs=${exoPlayer.currentPosition} targetMs=$targetMs durationMs=$durationMs playerDurationMs=${exoPlayer.duration} seekable=$seekable state=${playbackStateName(exoPlayer.playbackState)} isPlaying=${exoPlayer.isPlaying} playWhenReady=${exoPlayer.playWhenReady} bufferedMs=${exoPlayer.bufferedPosition}",
        )
        if (!seekable) {
            Timber.w(
                "FindroidCarVideoScreen seek attempting despite non-seekable flag source=%s sourceType=%s targetMs=%s durationMs=%s playerDurationMs=%s state=%s mediaItem=%s reason=match-standard-player-native-seek",
                source,
                item.playbackSourceKind(),
                targetMs,
                durationMs,
                exoPlayer.duration,
                playbackStateName(exoPlayer.playbackState),
                exoPlayer.currentMediaItem?.localConfiguration?.uri?.toSanitizedLogString(),
            )
        }
        exoPlayer.seekTo(targetMs)
        logSeekApplied(source = source, targetMs = targetMs, durationMs = durationMs)
        postPlaybackProgress(isPaused = !isPlaying)
        presentation?.updateProgress()
        showControlsTemporarily()
    }

    private fun logSeekApplied(source: String, targetMs: Long, durationMs: Long) {
        mainHandler.post {
            val exoPlayer = player
            Timber.i(
                "FindroidCarVideoScreen %s applied targetMs=%s actualPositionMs=%s playerPositionMs=%s durationMs=%s seekable=%s state=%s isPlaying=%s",
                source,
                targetMs,
                exoPlayer?.let(::absolutePlaybackPositionMs),
                exoPlayer?.currentPosition,
                durationMs,
                exoPlayer?.isCurrentMediaItemSeekable,
                exoPlayer?.playbackState?.let(::playbackStateName),
                exoPlayer?.isPlaying,
            )
            Log.i(
                FINDROID_CAR_AA_LOG_TAG,
                "FindroidCarVideoScreen $source applied targetMs=$targetMs actualPositionMs=${exoPlayer?.let(::absolutePlaybackPositionMs)} playerPositionMs=${exoPlayer?.currentPosition} durationMs=$durationMs seekable=${exoPlayer?.isCurrentMediaItemSeekable} state=${exoPlayer?.playbackState?.let(::playbackStateName)} isPlaying=${exoPlayer?.isPlaying}",
            )
        }
        mainHandler.postDelayed(
            {
                val exoPlayer = player
                val actualMs = exoPlayer?.let(::absolutePlaybackPositionMs)
                Timber.i(
                    "FindroidCarVideoScreen %s settled targetMs=%s actualPositionMs=%s playerPositionMs=%s deltaMs=%s durationMs=%s seekable=%s state=%s isPlaying=%s",
                    source,
                    targetMs,
                    actualMs,
                    exoPlayer?.currentPosition,
                    actualMs?.let { kotlin.math.abs(it - targetMs) },
                    durationMs,
                    exoPlayer?.isCurrentMediaItemSeekable,
                    exoPlayer?.playbackState?.let(::playbackStateName),
                    exoPlayer?.isPlaying,
                )
                Log.i(
                    FINDROID_CAR_AA_LOG_TAG,
                    "FindroidCarVideoScreen $source settled targetMs=$targetMs actualPositionMs=$actualMs playerPositionMs=${exoPlayer?.currentPosition} deltaMs=${actualMs?.let { kotlin.math.abs(it - targetMs) }} durationMs=$durationMs seekable=${exoPlayer?.isCurrentMediaItemSeekable} state=${exoPlayer?.playbackState?.let(::playbackStateName)} isPlaying=${exoPlayer?.isPlaying}",
                )
                presentation?.updateProgress()
            },
            SEEK_SETTLED_LOG_DELAY_MS,
        )
    }

    private data class SurfaceControlAction(
        val name: String,
        val centerX: Float,
        val invoke: () -> Unit,
    )

    private fun createPresentation(
        surfaceContainer: SurfaceContainer
    ): FindroidCarVideoPresentation? {
        val displayManager =
            carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        virtualDisplay =
            displayManager.createVirtualDisplay(
                "FindroidCarVideo",
                surfaceContainer.width,
                surfaceContainer.height,
                surfaceContainer.dpi,
                surfaceContainer.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            )
        val display = virtualDisplay?.display ?: return null
        Timber.i(
            "FindroidCarVideoScreen virtual display created: %sx%s dpi=%s",
            surfaceContainer.width,
            surfaceContainer.height,
            surfaceContainer.dpi,
        )
        return FindroidCarVideoPresentation(
            carContext,
            display,
            surfaceContainer.width,
            surfaceContainer.height,
        )
    }

    private fun releasePresentation() {
        presentation?.dismiss()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }

    private fun carIcon(resId: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()

    private fun movieAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

    private fun FindroidCarCatalogItem.playbackUri(): Uri? =
        when {
            !videoPath.isNullOrBlank() -> Uri.fromFile(File(videoPath))
            !streamUrl.isNullOrBlank() -> Uri.parse(streamUrl)
            else -> null
        }

    private fun FindroidCarCatalogItem.toCarMediaItem(uri: Uri): MediaItem =
        MediaItem.Builder()
            .setMediaId(itemId)
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()

    private fun FindroidCarCatalogItem.playbackSourceTypeForLog(): String =
        if (!videoPath.isNullOrBlank()) "local" else "online"

    private fun FindroidCarCatalogItem.playbackSourceForLog(): String =
        when {
            !videoPath.isNullOrBlank() -> videoPath.orEmpty()
            !streamUrl.isNullOrBlank() ->
                runCatching {
                        Uri.parse(streamUrl)
                            .buildUpon()
                            .clearQuery()
                            .fragment(null)
                            .build()
                            .toString()
                    }
                    .getOrDefault("online-stream")
            else -> "unavailable"
        }

    private fun effectiveDurationMs(exoPlayer: ExoPlayer): Long =
        exoPlayer.duration.takeIf { it > 0L && it != C.TIME_UNSET }
            ?: item.runtimeTicks.toMillis()

    private fun absolutePlaybackPositionMs(exoPlayer: ExoPlayer): Long =
        FindroidCarPlaybackTimeline.absolutePositionMs(
            playerPositionMs = exoPlayer.currentPosition,
            streamStartPositionMs = 0L,
            durationMs = effectiveDurationMs(exoPlayer),
        )

    private fun seekbarTrackLeft(): Float {
        val left = visibleArea?.left?.toFloat() ?: 0f
        return left + SEEK_BAR_HORIZONTAL_INSET
    }

    private fun seekbarTrackRight(): Float {
        val right =
            visibleArea?.right?.toFloat()
                ?: surfaceContainer?.width?.toFloat()
                ?: return 0f
        return right - SEEK_BAR_HORIZONTAL_INSET
    }

    private fun seekbarTrackWidth(): Float =
        (seekbarTrackRight() - seekbarTrackLeft()).coerceAtLeast(0f)

    private fun playerListener(): Player.Listener =
        object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val displayAspect =
                    if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio
                    else 1f
                val aspectMode =
                    presentation?.setVideoSize(
                        videoSize.width,
                        videoSize.height,
                        videoSize.pixelWidthHeightRatio,
                        aspectMode,
                    ) ?: "UNKNOWN"
                Timber.i(
                    "FindroidCarVideoScreen video size changed: %sx%s par=%s dar=%s aspectMode=%s",
                    videoSize.width,
                    videoSize.height,
                    videoSize.pixelWidthHeightRatio,
                    (videoSize.width * displayAspect) / videoSize.height.coerceAtLeast(1),
                    aspectMode,
                )
                Log.i(
                    FINDROID_CAR_AA_LOG_TAG,
                    "FindroidCarVideoScreen video size changed: ${videoSize.width}x${videoSize.height} par=${videoSize.pixelWidthHeightRatio} aspectMode=$aspectMode",
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    stopPlaybackReporting()
                    releasePlayer()
                    releasePresentation()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                Timber.i(
                    "FindroidCarVideoScreen position discontinuity reason=%s oldMs=%s newMs=%s actualPositionMs=%s playerPositionMs=%s seekable=%s state=%s isPlaying=%s",
                    discontinuityReasonName(reason),
                    oldPosition.positionMs,
                    newPosition.positionMs,
                    player?.let(::absolutePlaybackPositionMs),
                    player?.currentPosition,
                    player?.isCurrentMediaItemSeekable,
                    player?.playbackState?.let(::playbackStateName),
                    player?.isPlaying,
                )
                Log.i(
                    FINDROID_CAR_AA_LOG_TAG,
                    "FindroidCarVideoScreen position discontinuity reason=${discontinuityReasonName(reason)} oldMs=${oldPosition.positionMs} newMs=${newPosition.positionMs} actualPositionMs=${player?.let(::absolutePlaybackPositionMs)} playerPositionMs=${player?.currentPosition} seekable=${player?.isCurrentMediaItemSeekable} state=${player?.playbackState?.let(::playbackStateName)} isPlaying=${player?.isPlaying}",
                )
                presentation?.updateProgress()
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.e(
                    error,
                    "FindroidCarVideoScreen player error sourceType=%s source=%s errorCodeName=%s errorCode=%s causeClass=%s causeMessage=%s",
                    item.playbackSourceTypeForLog(),
                    item.playbackSourceForLog(),
                    error.errorCodeName,
                    error.errorCode,
                    error.cause?.javaClass?.name,
                    error.cause?.message,
                )
                Log.e(
                    FINDROID_CAR_AA_LOG_TAG,
                    "FindroidCarVideoScreen player error sourceType=${item.playbackSourceTypeForLog()} errorCodeName=${error.errorCodeName} errorCode=${error.errorCode} causeClass=${error.cause?.javaClass?.name} causeMessage=${error.cause?.message}",
                )
            }
        }

    private fun postPlaybackStart() {
        val id = item.uuidOrNull() ?: return
        scope.launch(Dispatchers.IO) { runCatching { jellyfinRepository.postPlaybackStart(id) } }
    }

    private fun postPlaybackProgress(isPaused: Boolean) {
        val id = item.uuidOrNull() ?: return
        val exoPlayer = player ?: return
        val positionTicks = absolutePlaybackPositionMs(exoPlayer).toTicks()
        FindroidCarPlaybackHistory.record(carContext, item, positionTicks)
        scope.launch(Dispatchers.IO) {
            runCatching {
                jellyfinRepository.postPlaybackProgress(
                    itemId = id,
                    positionTicks = positionTicks,
                    isPaused = isPaused,
                    favorite = item.favorite,
                )
            }
        }
    }

    private fun stopPlaybackReporting() {
        val id = item.uuidOrNull() ?: return
        val exoPlayer = player ?: return
        val duration = effectiveDurationMs(exoPlayer)
        val position = absolutePlaybackPositionMs(exoPlayer).coerceAtLeast(0L)
        val playedPercentage =
            if (duration > 0L) ((position * 100) / duration).toInt() else 0
        if (playedPercentage in 10..90) {
            FindroidCarPlaybackHistory.record(carContext, item, position.toTicks())
        } else {
            FindroidCarPlaybackHistory.clear(carContext, item.itemId)
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                jellyfinRepository.postPlaybackStop(
                    itemId = id,
                    positionTicks = position.toTicks(),
                    playedPercentage = playedPercentage,
                    favorite = item.favorite,
                )
            }
        }
    }

    private fun FindroidCarCatalogItem.uuidOrNull(): UUID? =
        runCatching { UUID.fromString(itemId) }.getOrNull()

    private fun FindroidCarCatalogItem.startPositionMs(): Long =
        findroidCarStartPositionMs(playbackPositionTicks)

    private fun Long.toTicks(): Long = this * TICKS_PER_MS

    private fun Long.toMillis(): Long = this / TICKS_PER_MS

    private fun playbackStateName(state: Int): String =
        when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> state.toString()
        }

    private fun discontinuityReasonName(reason: Int): String =
        when (reason) {
            Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "AUTO_TRANSITION"
            Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
            Player.DISCONTINUITY_REASON_SKIP -> "SKIP"
            Player.DISCONTINUITY_REASON_REMOVE -> "REMOVE"
            Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
            else -> reason.toString()
        }

    private fun Uri.toSanitizedLogString(): String =
        if (scheme == "http" || scheme == "https") {
            buildUpon().clearQuery().fragment(null).build().toString()
        } else {
            toString()
        }

    private companion object {
        const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
        const val PLAYBACK_PROGRESS_INTERVAL_MS = 10_000L
        const val SEEK_BACK_MS = 10_000L
        const val SEEK_FORWARD_MS = 30_000L
        const val TICKS_PER_MS = 10_000L
        const val CONTROL_BUTTON_HIT_HEIGHT = 110f
        const val CONTROL_BUTTON_HIT_RADIUS = 44f
        const val REWIND_BUTTON_OFFSET_FROM_VISIBLE_RIGHT = 248f
        const val PLAY_PAUSE_BUTTON_OFFSET_FROM_VISIBLE_RIGHT = 172f
        const val FORWARD_BUTTON_OFFSET_FROM_VISIBLE_RIGHT = 96f
        const val BACK_BUTTON_OFFSET_FROM_VISIBLE_RIGHT = 20f
        const val ASPECT_BUTTON_OFFSET_FROM_VISIBLE_LEFT = 72f
        const val ASPECT_BUTTON_HIT_WIDTH = 160f
        const val ASPECT_BUTTON_TOP = 16f
        const val ASPECT_BUTTON_BOTTOM = 112f
        const val SEEK_BAR_HORIZONTAL_INSET = 98f
        const val SEEK_BAR_CENTER_OFFSET_FROM_VISIBLE_BOTTOM = 58f
        const val SEEK_BAR_HIT_VERTICAL_RADIUS = 38f
        const val MIN_SCROLL_SEEK_MS = 500L
        const val SEEK_SETTLED_LOG_DELAY_MS = 300L
        const val FINDROID_CAR_AA_LOG_TAG = "FindroidCarAA"
    }
}
