package dev.jdtech.jellyfin.utils

import android.media.AudioManager
import android.provider.Settings
import android.provider.SyncStateContract
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
import com.google.android.exoplayer2.ui.PlayerView
import dev.jdtech.jellyfin.PlayerActivity
import timber.log.Timber
import kotlin.math.abs

class PlayerGestureHelper(
    private val activity: PlayerActivity,
    private val playerView: PlayerView,
    private val audioManager: AudioManager
) {
    /**
     * Tracks a value during a swipe gesture (between multiple onScroll calls).
     * When the gesture starts it's reset to an initial value and gets increased or decreased
     * (depending on the direction) as the gesture progresses.
     */
    private var swipeGestureValueTrackerVolume = -1f
    private var swipeGestureValueTrackerBrightness = -1f

    private val gestureDetector = GestureDetector(playerView.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            playerView.apply {
                if (!isControllerVisible) showController() else hideController()
            }
            return true
        }

        override fun onScroll(firstEvent: MotionEvent, currentEvent: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Check whether swipe was oriented vertically
            if (abs(distanceY / distanceX) < 2)
                return false

            if (firstEvent.y < playerView.resources.dip(Constants.GESTURE_EXCLUSION_AREA_TOP))
                return false


            val viewCenterX = playerView.measuredWidth / 2

            // Distance to swipe to go from min to max
            val distanceFull = playerView.measuredHeight * Constants.FULL_SWIPE_RANGE_SCREEN_RATIO
            val ratioChange = distanceY / distanceFull

            if (firstEvent.x.toInt() > viewCenterX) {
                // Swiping on the right, change volume

                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (swipeGestureValueTrackerVolume == -1f) swipeGestureValueTrackerVolume = currentVolume.toFloat()

                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val change = ratioChange * maxVolume
                swipeGestureValueTrackerVolume += change

                val toSet = swipeGestureValueTrackerVolume.toInt().coerceIn(0, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, toSet, 0)

                activity.binding.gestureVolumeLayout.visibility = View.VISIBLE
                activity.binding.gestureVolumeProgressBar.max = maxVolume
                activity.binding.gestureVolumeProgressBar.progress = toSet
                activity.binding.gestureVolumeText.text = "${(toSet.toFloat()/maxVolume.toFloat()).times(100).toInt()}%"
            } else {
                // Swiping on the left, change brightness
                val window = activity.window
                val brightnessRange = BRIGHTNESS_OVERRIDE_OFF..BRIGHTNESS_OVERRIDE_FULL

                // Initialize on first swipe
                if (swipeGestureValueTrackerBrightness == -1f) {
                    val brightness = window.attributes.screenBrightness
                    Timber.d("Brightness ${Settings.System.getFloat(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)}")
                    swipeGestureValueTrackerBrightness = when (brightness) {
                        in brightnessRange -> brightness
                        else -> Settings.System.getFloat(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)/255
                    }
                }
                swipeGestureValueTrackerBrightness = (swipeGestureValueTrackerBrightness + ratioChange).coerceIn(brightnessRange)
                val lp = window.attributes
                lp.screenBrightness = swipeGestureValueTrackerBrightness
                window.attributes = lp

                activity.binding.gestureBrightnessLayout.visibility = View.VISIBLE
                activity.binding.gestureBrightnessProgressBar.max = BRIGHTNESS_OVERRIDE_FULL.times(100).toInt()
                activity.binding.gestureBrightnessProgressBar.progress = lp.screenBrightness.times(100).toInt()
                activity.binding.gestureBrightnessText.text = "${(lp.screenBrightness/BRIGHTNESS_OVERRIDE_FULL).times(100).toInt()}%"
            }
            return true
        }
    })

    private val hideGestureVolumeIndicatorOverlayAction = Runnable {
        activity.binding.gestureVolumeLayout.visibility = View.GONE
    }

    private val hideGestureBrightnessIndicatorOverlayAction = Runnable {
        activity.binding.gestureBrightnessLayout.visibility = View.GONE
    }

    init {
        @Suppress("ClickableViewAccessibility")
        playerView.setOnTouchListener { _, event ->
            if (playerView.useController) {
                when (event.pointerCount) {
                    1 -> gestureDetector.onTouchEvent(event)
                }
            }
            if(event.action == MotionEvent.ACTION_UP) {
                activity.binding.gestureVolumeLayout.apply {
                    if (visibility == View.VISIBLE) {
                        removeCallbacks(hideGestureVolumeIndicatorOverlayAction)
                        postDelayed(hideGestureVolumeIndicatorOverlayAction, 1000)
                    }
                }
                activity.binding.gestureBrightnessLayout.apply {
                    if (visibility == View.VISIBLE) {
                        removeCallbacks(hideGestureBrightnessIndicatorOverlayAction)
                        postDelayed(hideGestureBrightnessIndicatorOverlayAction, 1000)
                    }
                }
            }
            true
        }
    }
}
