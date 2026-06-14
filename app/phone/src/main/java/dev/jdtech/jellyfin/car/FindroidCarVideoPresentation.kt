package dev.jdtech.jellyfin.car

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.roundToInt
import timber.log.Timber

class FindroidCarVideoPresentation(
    context: Context,
    display: Display,
    private val surfaceWidth: Int,
    private val surfaceHeight: Int,
) : Presentation(context, display) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressRunnable =
        object : Runnable {
            override fun run() {
                updateProgress()
                mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }

    private var player: ExoPlayer? = null
    private lateinit var aspectFrame: AspectRatioFrameLayout
    private lateinit var textureView: TextureView
    private lateinit var aspectText: TextView
    private lateinit var controlsOverlay: LinearLayout
    private lateinit var progressView: VideoProgressView
    private lateinit var timeText: TextView
    private var fallbackDurationMs = 0L
    private var positionOffsetMs = 0L
    private var timelineDurationMs = 0L
    private var aspectMode = FindroidCarVideoAspectMode.AUTO
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoPixelWidthHeightRatio = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        textureView =
            TextureView(context).apply {
                isOpaque = true
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
            }

        aspectFrame =
            AspectRatioFrameLayout(context).apply {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(Color.BLACK)
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                addView(textureView)
            }

        aspectText =
            TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background =
                    GradientDrawable().apply {
                        setColor(0x99000000.toInt())
                        cornerRadius = dp(20).toFloat()
                    }
                visibility = View.GONE
                layoutParams =
                    FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.START,
                        )
                        .apply {
                            leftMargin = dp(84)
                            topMargin = dp(28)
                        }
            }

        progressView =
            VideoProgressView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(PROGRESS_HEIGHT_DP),
                    )
            }

        timeText =
            TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.CENTER_HORIZONTAL
                text = "00:00 / --:--"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }

        controlsOverlay =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(16), dp(8), dp(16), dp(10))
                setBackgroundColor(0x66000000)
                visibility = View.GONE
                layoutParams =
                    FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM,
                        )
                        .apply {
                            leftMargin = dp(72)
                            rightMargin = dp(72)
                            bottomMargin = dp(18)
                        }
                addView(progressView)
                addView(timeText)
            }

        setContentView(
            FrameLayout(context).apply {
                setBackgroundColor(Color.BLACK)
                addView(aspectFrame)
                addView(aspectText)
                addView(controlsOverlay)
            }
        )
        updateAspectText()
    }

    override fun dismiss() {
        stopProgressUpdates()
        setPlayer(null)
        super.dismiss()
    }

    fun setPlayer(player: ExoPlayer?) {
        this.player?.clearVideoTextureView(textureView)
        this.player = player
        player?.setVideoTextureView(textureView)
        updateProgress()
    }

    fun setControlsVisible(visible: Boolean) {
        aspectText.visibility = if (visible) View.VISIBLE else View.GONE
        controlsOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) startProgressUpdates() else stopProgressUpdates()
        updateProgress()
    }

    fun setFallbackDuration(durationMs: Long) {
        fallbackDurationMs = durationMs.coerceAtLeast(0L)
        updateProgress()
    }

    fun setPlaybackTimeline(positionOffsetMs: Long, durationMs: Long) {
        this.positionOffsetMs = positionOffsetMs.coerceAtLeast(0L)
        timelineDurationMs = durationMs.coerceAtLeast(0L)
        updateProgress()
    }

    fun setAspectMode(mode: FindroidCarVideoAspectMode): String {
        aspectMode = mode
        updateAspectText()
        return applyAspectMode()
    }

    fun setVideoSize(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float,
        mode: FindroidCarVideoAspectMode,
    ): String {
        videoWidth = width
        videoHeight = height
        videoPixelWidthHeightRatio = pixelWidthHeightRatio
        aspectMode = mode
        updateAspectText()
        return applyAspectMode()
    }

    private fun applyAspectMode(): String {
        if (videoWidth <= 0 || videoHeight <= 0 || surfaceWidth <= 0 || surfaceHeight <= 0) {
            aspectFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            updateAspectText()
            return "FIT"
        }
        val par = if (videoPixelWidthHeightRatio > 0f) videoPixelWidthHeightRatio else 1f
        val sourceDar = (videoWidth * par) / videoHeight
        val surfaceDar = surfaceWidth.toFloat() / surfaceHeight
        val fillCropFraction =
            if (sourceDar > surfaceDar) 1f - (surfaceDar / sourceDar)
            else 1f - (sourceDar / surfaceDar)
        val useZoom =
            when (aspectMode) {
                FindroidCarVideoAspectMode.AUTO -> fillCropFraction <= SMART_CROP_MAX_FRACTION
                FindroidCarVideoAspectMode.FIT -> false
                FindroidCarVideoAspectMode.CROP -> true
            }
        aspectFrame.resizeMode =
            if (useZoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else AspectRatioFrameLayout.RESIZE_MODE_FIT
        aspectFrame.setAspectRatio(sourceDar)
        val appliedMode =
            when {
                aspectMode == FindroidCarVideoAspectMode.AUTO && useZoom -> "AUTO_SMART_ZOOM"
                aspectMode == FindroidCarVideoAspectMode.AUTO -> "AUTO_FIT"
                aspectMode == FindroidCarVideoAspectMode.CROP -> "CROP_FULLSCREEN"
                else -> "FIT"
            }
        Timber.i(
            "FindroidCarVideoPresentation aspect source=%sx%s par=%s sourceDar=%s surface=%sx%s surfaceDar=%s crop=%s requestedMode=%s appliedMode=%s",
            videoWidth,
            videoHeight,
            par,
            sourceDar,
            surfaceWidth,
            surfaceHeight,
            surfaceDar,
            fillCropFraction,
            aspectMode.name,
            appliedMode,
        )
        return appliedMode
    }

    fun updateProgress() {
        val player = player ?: return
        val duration =
            timelineDurationMs.takeIf { it > 0L }
                ?: player.duration.takeIf { it > 0L }?.let { it + positionOffsetMs }
                ?: fallbackDurationMs
        val position =
            FindroidCarPlaybackTimeline.absolutePositionMs(
                playerPositionMs = player.currentPosition,
                streamStartPositionMs = positionOffsetMs,
                durationMs = duration,
            )
        progressView.setProgress(position, duration)
        if (duration > 0L) {
            timeText.text = "${formatTime(position)} / ${formatTime(duration)}"
        } else {
            timeText.text = "${formatTime(position)} / --:--"
        }
    }

    private fun startProgressUpdates() {
        mainHandler.removeCallbacks(progressRunnable)
        mainHandler.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        mainHandler.removeCallbacks(progressRunnable)
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0L) / 1000L
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    private fun updateAspectText() {
        if (::aspectText.isInitialized) {
            aspectText.text = aspectMode.label
        }
    }

    private companion object {
        const val PROGRESS_HEIGHT_DP = 28
        const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        const val SMART_CROP_MAX_FRACTION = 0.08f
    }
}

enum class FindroidCarVideoAspectMode(val label: String) {
    AUTO("AUTO"),
    FIT("FIT"),
    CROP("CROP"),
    ;

    fun next(): FindroidCarVideoAspectMode =
        when (this) {
            AUTO -> FIT
            FIT -> CROP
            CROP -> AUTO
        }
}

private class VideoProgressView(context: Context) : View(context) {
    private val trackPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x77FFFFFF
            style = Paint.Style.FILL
        }
    private val playedPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    private val thumbPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    private val thumbStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCC000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }
    private val rect = RectF()
    private var positionMs = 0L
    private var durationMs = 0L

    fun setProgress(positionMs: Long, durationMs: Long) {
        this.positionMs = positionMs.coerceAtLeast(0L)
        this.durationMs = durationMs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val horizontalPadding = dp(10).toFloat()
        val trackHeight = dp(8).toFloat()
        val thumbRadius = dp(9).toFloat()
        val centerY = height / 2f
        val left = horizontalPadding
        val right = width - horizontalPadding
        val usableWidth = (right - left).coerceAtLeast(1f)
        val progress =
            if (durationMs > 0L) (positionMs.toDouble() / durationMs).coerceIn(0.0, 1.0).toFloat()
            else 0f
        val thumbX = left + usableWidth * progress

        rect.set(left, centerY - trackHeight / 2f, right, centerY + trackHeight / 2f)
        canvas.drawRoundRect(rect, trackHeight / 2f, trackHeight / 2f, trackPaint)
        rect.set(left, centerY - trackHeight / 2f, thumbX, centerY + trackHeight / 2f)
        canvas.drawRoundRect(rect, trackHeight / 2f, trackHeight / 2f, playedPaint)
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint)
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbStrokePaint)
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}
