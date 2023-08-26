package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.player.video.R
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import java.lang.IllegalStateException

class SpeedSelectionDialogFragment(
    private val viewModel: PlayerActivityViewModel,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val speedTexts = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")
        val speedNumbers = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

        return activity?.let { activity ->
            val builder = MaterialAlertDialogBuilder(activity)
            builder.setTitle(getString(R.string.select_playback_speed))
                .setSingleChoiceItems(
                    speedTexts.toTypedArray(),
                    speedNumbers.indexOf(viewModel.playbackSpeed),
                ) { dialog, which ->
                    viewModel.selectSpeed(
                        speedNumbers[which],
                    )
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
