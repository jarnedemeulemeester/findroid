package dev.jdtech.jellyfin.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import java.lang.IllegalStateException

class TrackSelectionDialogFragment(
    private val type: String,
    private val viewModel: PlayerActivityViewModel
): DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val trackNames: List<String>
        when (type) {
            TrackType.AUDIO -> {
                trackNames = viewModel.currentAudioTracks.map {
                    if (it.title.isEmpty()) {
                        "${it.lang} - ${it.codec}"
                    } else {
                        "${it.title} - ${it.lang} - ${it.codec}"
                    }
                }
                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("Select audio track")
                        .setItems(trackNames.toTypedArray()) { _, which ->
                            viewModel.switchToTrack(TrackType.AUDIO, viewModel.currentAudioTracks[which])
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
            TrackType.SUBTITLE -> {
                trackNames = viewModel.currentSubtitleTracks.map {
                    if (it.title.isEmpty()) {
                        "${it.lang} - ${it.codec}"
                    } else {
                        "${it.title} - ${it.lang} - ${it.codec}"
                    }
                }
                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("Select subtitle track")
                        .setItems(trackNames.toTypedArray()) { _, which ->
                            viewModel.switchToTrack(TrackType.SUBTITLE, viewModel.currentSubtitleTracks[which])
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
            else -> {
                trackNames = listOf()
                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("Select ? track")
                        .setMessage("Unknown track type")
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
        }
    }
}