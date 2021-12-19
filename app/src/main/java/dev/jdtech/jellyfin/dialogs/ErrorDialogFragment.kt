package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.R
import java.lang.IllegalStateException

class ErrorDialogFragment(private val errorMessage: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it, R.style.ErrorDialogStyle)
            builder
                .setMessage(errorMessage)
                .setPositiveButton(getString(R.string.close)) { _, _ ->
                }
                .setNeutralButton(getString(R.string.share)) { _, _ ->
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, errorMessage)
                        type = "text/plain"
                    }

                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)

                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}