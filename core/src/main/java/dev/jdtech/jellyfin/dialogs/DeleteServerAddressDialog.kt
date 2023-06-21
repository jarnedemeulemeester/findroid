package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.viewmodels.ServerAddressesViewModel
import java.lang.IllegalStateException

class DeleteServerAddressDialog(
    private val viewModel: ServerAddressesViewModel,
    val address: ServerAddress,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            builder.setTitle(getString(R.string.remove_server_address))
                .setMessage(getString(R.string.remove_server_address_dialog_text, address.address))
                .setPositiveButton(getString(R.string.remove)) { _, _ ->
                    viewModel.deleteAddress(address)
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
