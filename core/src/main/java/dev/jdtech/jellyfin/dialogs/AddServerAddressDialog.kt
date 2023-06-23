package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.viewmodels.ServerAddressesViewModel
import java.lang.IllegalStateException

class AddServerAddressDialog(
    private val viewModel: ServerAddressesViewModel,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(this.context)
        editText.hint = "http://<server_ip>:8096"
        return activity?.let { activity ->
            val builder = MaterialAlertDialogBuilder(activity)
            builder
                .setTitle(getString(R.string.add_server_address))
                .setView(editText)
                .setPositiveButton(getString(R.string.add)) { _, _ ->
                    viewModel.addAddress(editText.text.toString())
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
