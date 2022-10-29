package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.R
import java.lang.IllegalStateException

class ErrorDialogFragment(
    private val error: Exception
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it, R.style.ErrorDialogStyle)
            builder
                .setTitle(error.message ?: getString(R.string.unknown_error))
                .setMessage(error.stackTraceToString())
                .setPositiveButton(getString(R.string.close)) { _, _ ->
                }
                .setNeutralButton(getString(R.string.share)) { _, _ ->
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "${error.message}\n ${error.stackTraceToString()}")
                        type = "text/plain"
                    }

                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
