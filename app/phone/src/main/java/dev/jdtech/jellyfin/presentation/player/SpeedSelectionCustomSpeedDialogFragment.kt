package dev.jdtech.jellyfin.presentation.player

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.player.local.R
import kotlin.math.exp
import kotlin.math.ln

class SpeedSelectionCustomSpeedDialogFragment(
    private val speedSelectionDialog: SpeedSelectionDialogFragment,
    private val currentSpeed: Float,
) : DialogFragment() {

    /**
     * Define the key values for the speed selection slide bar. Chosen for the logarithmic scaling.
     */
    private object SeekBarConstants {
        private const val MAX_SPEED = 4.01f
        private const val MIN_SPEED = 1 / 4f
        const val NORMALIZATION = 2000
        val MAX = (NORMALIZATION * ln(MAX_SPEED)).toInt()
        val MIN = (NORMALIZATION * ln(MIN_SPEED)).toInt()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->

            val speedText = TextView(activity.baseContext)
            speedText.text = createLabel(currentSpeed)
            speedText.gravity = Gravity.CENTER

            val seekBar = SeekBar(activity.baseContext)
            seekBar.min = SeekBarConstants.MIN
            seekBar.max = SeekBarConstants.MAX
            seekBar.progress = speedToSeekBarValue(currentSpeed)

            val listener = object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    speedText.text = createLabel(seekBarValueToSpeed(seekBar?.progress ?: 0))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {} // NO-OP

                override fun onStopTrackingTouch(seekBar: SeekBar?) {} // NO-OP
            }
            seekBar.setOnSeekBarChangeListener(listener)

            val container = LinearLayout(activity.baseContext)
            container.orientation = LinearLayout.VERTICAL
            container.addView(speedText)
            container.addView(seekBar)

            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.custom_playback_speed)
                .setView(container)
                .setPositiveButton(R.string.custom_playback_speed_confirm_button_label) { dialog, _ ->
                    speedSelectionDialog.setCustomSpeed(seekBarValueToSpeed(seekBar.progress))
                    dialog.dismiss()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    /**
     * Scale the integer value from the SeekBar to the associated playback speed multiplier.
     * Uses a logarithmic scale so that X speed and 1/X speed are equidistant from 1x speed.
     * Discards precision beyond 2 decimal places.
     * Inverted by [speedToSeekBarValue].
     */
    private fun seekBarValueToSpeed(int: Int): Float {
        val preciseSpeed = exp((int.toFloat() / SeekBarConstants.NORMALIZATION))
        return ((100 * preciseSpeed).toInt() / 100f)
    }

    /**
     * Scale a float playback speed multiplier to the associated progress value for the SeekBar.
     * Uses a logarithmic scale so that X speed and 1/X speed are equidistant from 1x speed.
     * Inverted by [seekBarValueToSpeed].
     */
    private fun speedToSeekBarValue(float: Float): Int {
        return (SeekBarConstants.NORMALIZATION * ln(float)).toInt()
    }

    /**
     * Create a formatted string for a label describing the selected playback speed multiplier.
     */
    private fun createLabel(float: Float): String {
        return "%.2fx".format(float)
    }
}
