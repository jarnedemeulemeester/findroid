package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.player.video.R
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import java.lang.IllegalStateException

class TrackSelectionDialogFragment(
    private val type: TrackType,
    private val viewModel: PlayerActivityViewModel,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        when (type) {
            TrackType.AUDIO -> {
                return activity?.let { activity ->
                    val builder = MaterialAlertDialogBuilder(activity)
                    builder.setTitle(getString(R.string.select_audio_track))
                        .setSingleChoiceItems(
                            getTrackNames(viewModel.currentAudioTracks),
                            viewModel.currentAudioTracks.indexOfFirst { it.selected },
                        ) { dialog, which ->
                            viewModel.switchToTrack(
                                TrackType.AUDIO,
                                viewModel.currentAudioTracks[which],
                            )
                            dialog.dismiss()
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
            TrackType.SUBTITLE -> {
                return activity?.let { activity ->
                    val builder = MaterialAlertDialogBuilder(activity)
                    builder.setTitle(getString(R.string.select_subtile_track))
                        .setSingleChoiceItems(
                            getTrackNames(viewModel.currentSubtitleTracks),
                            viewModel.currentSubtitleTracks.indexOfFirst { if (viewModel.disableSubtitle) it.ffIndex == -1 else it.selected },
                        ) { dialog, which ->
                            viewModel.switchToTrack(
                                TrackType.SUBTITLE,
                                viewModel.currentSubtitleTracks[which],
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

    private fun getTrackNames(tracks: List<MPVPlayer.Companion.Track>): Array<String> {
        return tracks.map { track ->
            val nameParts: MutableList<String> = mutableListOf()
            if (track.title.isNotEmpty()) nameParts.add(track.title)
            if (track.lang.isNotEmpty()) nameParts.add(track.lang)
            if (track.codec.isNotEmpty()) nameParts.add(track.codec)
            nameParts.joinToString(separator = " - ")
        }.toTypedArray()
    }
}
