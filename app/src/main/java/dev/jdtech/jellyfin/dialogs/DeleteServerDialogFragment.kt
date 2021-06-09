package dev.jdtech.jellyfin.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.viewmodels.ServerSelectViewModel
import java.lang.IllegalStateException

class DeleteServerDialogFragment(private val viewModel: ServerSelectViewModel, val server: Server) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(getString(R.string.remove_server))
                .setMessage(getString(R.string.remove_server_dialog_text, server.name))
                .setPositiveButton(getString(R.string.remove)) { _, _ ->
                    viewModel.deleteServer(server)
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->

                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}