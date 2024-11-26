package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.player.video.R
import java.util.Locale

class SpeedSelectionCustomSpeedDialogFragment(
    private val speedSelectionDialog: SpeedSelectionDialogFragment,
    private val currentSpeed: Float
): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let { activity ->

            val speedText = TextView(activity.baseContext)
            speedText.text = createLabel(currentSpeed)
            speedText.gravity = Gravity.CENTER

            // Use a SeekBar with a range scaled 100x from the intended playback speed multiplier.
            val seekBar = SeekBar(activity.baseContext)
            seekBar.min = 25
            seekBar.max = 400
            seekBar.progress = speedToSeekBarValue(currentSpeed)

            val listener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    speedText.text = createLabel(seekBarValueToSpeed(seekBar?.progress ?: 100))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // NO-OP
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // NO-OP
                }
            }
            seekBar.setOnSeekBarChangeListener(listener)

            val container = LinearLayout(activity.baseContext)
            container.orientation = LinearLayout.VERTICAL
            container.addView(speedText)
            container.addView(seekBar)

            MaterialAlertDialogBuilder(activity)
                .setTitle(getString(R.string.custom_playback_speed))
                .setView(container)
                .setPositiveButton(R.string.custom_playback_speed_confirm_button_label) { dialog, _ ->
                    speedSelectionDialog.setCustomSpeed(seekBarValueToSpeed(seekBar.progress))
                    dialog.dismiss()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null");
    }

    /**
     * Scale the integer value from the SeekBar to the associated playback speed multiplier.
     * Inverted by [speedToSeekBarValue].
     */
    private fun seekBarValueToSpeed(int: Int): Float {
        return int.toFloat() / 100
    }

    /**
     * Scale a float playback speed multiplier to the associated progress value for the SeekBar.
     * Inverted by [seekBarValueToSpeed].
     */
    private fun speedToSeekBarValue(float: Float): Int {
        return (float * 100).toInt()
    }

    /**
     * Create a formatted string for a label describing the selected playback speed multiplier.
     */
    private fun createLabel(float: Float): String {
        return String.format(Locale.getDefault(), "%.2f x", float)
    }
}