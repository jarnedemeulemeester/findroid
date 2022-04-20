package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import java.lang.IllegalStateException

class TrackSelectionDialogFragment(
    private val type: String,
    private val viewModel: PlayerActivityViewModel
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val trackNames: List<String>
        when (type) {
            TrackType.AUDIO -> {
                trackNames = viewModel.currentAudioTracks.map { track ->
                    if (track.title.isEmpty()) {
                        "${track.lang} - ${track.codec}"
                    } else {
                        "${track.title} - ${track.lang} - ${track.codec}"
                    }
                }
                return activity?.let { activity ->
                    val builder = MaterialAlertDialogBuilder(activity)
                    builder.setTitle(getString(R.string.select_audio_track))
                        .setSingleChoiceItems(
                            trackNames.toTypedArray(),
                            viewModel.currentAudioTracks.indexOfFirst { it.selected }) { dialog, which ->
                            viewModel.switchToTrack(
                                TrackType.AUDIO,
                                viewModel.currentAudioTracks[which]
                            )
                            dialog.dismiss()
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
            TrackType.SUBTITLE -> {
                trackNames = viewModel.currentSubtitleTracks.map { track ->
                    if (track.title.isEmpty()) {
                        "${track.lang} - ${track.codec}"
                    } else if (track.title.isNotEmpty() && track.lang.isEmpty() && track.codec.isEmpty()) {
                        track.title
                    } else {
                        "${track.title} - ${track.lang} - ${track.codec}"
                    }
                }
                return activity?.let { activity ->
                    val builder = MaterialAlertDialogBuilder(activity)
                    builder.setTitle(getString(R.string.select_subtile_track))
                        .setSingleChoiceItems(
                            trackNames.toTypedArray(),
                            viewModel.currentSubtitleTracks.indexOfFirst { if (viewModel.noSubtitle) it.ffIndex == -1 else it.selected }) { dialog, which ->
                            viewModel.switchToTrack(
                                TrackType.SUBTITLE,
                                viewModel.currentSubtitleTracks[which]
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