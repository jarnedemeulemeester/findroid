package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.utils.serializable
import java.io.Serializable
import java.lang.IllegalStateException

class ErrorDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val error = requireArguments().serializable<Exception>("error")!!
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

    companion object {
        const val TAG = "error_dialog"

        fun newInstance(error: Exception): ErrorDialogFragment {
            val errorDialogFragment = ErrorDialogFragment()
            val args = Bundle()
            args.putSerializable("error", error as Serializable)
            errorDialogFragment.arguments = args
            return errorDialogFragment
        }
    }
}
