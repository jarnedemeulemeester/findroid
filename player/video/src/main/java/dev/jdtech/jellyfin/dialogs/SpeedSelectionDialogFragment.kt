package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.player.video.R
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import java.util.Locale

class SpeedSelectionDialogFragment(
    private val viewModel: PlayerActivityViewModel,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customLabel = getString(R.string.custom_playback_speed_label)
        val currentSpeed = viewModel.playbackSpeed

        val speedTexts = mutableListOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")
        val speedNumbers = mutableListOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

        if (currentSpeed !in speedNumbers) {
            speedTexts.add("%s: %.2fx".format(Locale.getDefault(), customLabel, currentSpeed))
            speedNumbers.add(currentSpeed)
        }
        else {
            speedTexts.add(customLabel)
        }

        return activity?.let { activity ->
            val builder = MaterialAlertDialogBuilder(activity)
            builder.setTitle(getString(R.string.select_playback_speed))
                .setSingleChoiceItems(
                    speedTexts.toTypedArray(),
                    speedNumbers.indexOf(viewModel.playbackSpeed),
                ) { dialog, which ->
                    if (speedTexts[which].startsWith(customLabel)) {
                        // Use a secondary dialog to determine the speed to set.
                        SpeedSelectionCustomSpeedDialogFragment(this, currentSpeed)
                            .show(activity.supportFragmentManager, "customSpeedSelection")
                    }
                    else {
                        setCustomSpeed(speedNumbers[which])
                    }
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fix for hiding the system bars on API < 30
        activity?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun setCustomSpeed(speed: Float) {
        viewModel.selectSpeed(speed)
    }
}
