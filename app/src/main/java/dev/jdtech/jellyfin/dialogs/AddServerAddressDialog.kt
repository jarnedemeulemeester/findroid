package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.viewmodels.ServerAddressesViewModel
import java.lang.IllegalStateException

class AddServerAddressDialog(
    private val viewModel: ServerAddressesViewModel
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val uiModeManager =
            requireContext().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager
        val editText = EditText(this.context)
        return activity?.let { activity ->
            val builder = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                AlertDialog.Builder(activity)
            } else {
                MaterialAlertDialogBuilder(activity)
            }
            builder
                .setTitle("Add server address")
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
