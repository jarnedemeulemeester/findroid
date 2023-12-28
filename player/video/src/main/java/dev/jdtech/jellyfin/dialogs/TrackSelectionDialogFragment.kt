package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.media3.common.C
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.getTrackNames
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.player.video.R
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import java.lang.IllegalStateException

// TODO add option to disable track type
class TrackSelectionDialogFragment(
    private val type: TrackType,
    private val viewModel: PlayerActivityViewModel,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        when (type) {
            TrackType.AUDIO -> {
                return activity?.let { activity ->
                    val builder = MaterialAlertDialogBuilder(activity)
                    val tracksGroups = viewModel.player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                    builder.setTitle(getString(R.string.select_audio_track))
                        .setSingleChoiceItems(
                            tracksGroups.getTrackNames(),
                            tracksGroups.indexOfFirst { it.isSelected },
                        ) { dialog, which ->
                            viewModel.switchToTrack(
                                TrackType.AUDIO,
                                tracksGroups[which],
                            )
                            dialog.dismiss()
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
            TrackType.SUBTITLE -> {
                return activity?.let { activity ->
                    val builder = MaterialAlertDialogBuilder(activity)
                    val tracksGroups = viewModel.player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                    builder.setTitle(getString(R.string.select_subtile_track))
                        .setSingleChoiceItems(
                            tracksGroups.getTrackNames(),
                            tracksGroups.indexOfFirst { it.isSelected },
                        ) { dialog, which ->
                            viewModel.switchToTrack(
                                TrackType.SUBTITLE,
                                tracksGroups[which],
                            )
                            dialog.dismiss()
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
            else -> {
                throw IllegalStateException("TrackType must be AUDIO or SUBTITLE")
            }
        }
    }
}
