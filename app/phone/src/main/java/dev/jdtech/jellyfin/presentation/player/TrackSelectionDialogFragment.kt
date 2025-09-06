package dev.jdtech.jellyfin.presentation.player

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.media3.common.C
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.player.local.R
import dev.jdtech.jellyfin.player.local.domain.getTrackNames
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.lang.IllegalStateException

class TrackSelectionDialogFragment(
    private val type: @C.TrackType Int,
    private val viewModel: PlayerViewModel,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val titleResource = when (type) {
            C.TRACK_TYPE_AUDIO -> R.string.select_audio_track
            C.TRACK_TYPE_TEXT -> R.string.select_subtile_track
            else -> throw IllegalStateException("TrackType must be AUDIO or TEXT")
        }
        val tracksGroups = viewModel.player.currentTracks.groups.filter { it.type == type && it.isSupported }
        return activity?.let { activity ->
            val builder = MaterialAlertDialogBuilder(activity)
            builder
                .setTitle(getString(titleResource))
                .setSingleChoiceItems(
                    arrayOf(getString(R.string.none)) + tracksGroups.getTrackNames(), // Add "None" at the top of the list
                    tracksGroups.indexOfFirst { it.isSelected } + 1, // Add 1 to the index to account for the "None" item
                ) { dialog, which ->
                    viewModel.switchToTrack(
                        type,
                        which - 1, // Minus 1 to get the correct group without the "None" item. "None" becomes -1
                    )
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
}
