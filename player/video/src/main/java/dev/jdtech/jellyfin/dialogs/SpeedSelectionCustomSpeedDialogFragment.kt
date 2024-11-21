package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.jdtech.jellyfin.player.video.R

class SpeedSelectionCustomSpeedDialogFragment(
    private val speedSelectionDialog: SpeedSelectionDialogFragment
): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let { activity ->
            val textInputLayout = TextInputLayout(activity)
            val editText = TextInputEditText(textInputLayout.context)
            // Bitwise OR creates the input type for a decimal number.
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER.or(android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)

            val builtDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(getString(R.string.custom_playback_speed))
                .setView(editText).create()

            editText.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    val newSpeed = textView.text.toString().toFloatOrNull()
                    if (newSpeed != null && newSpeed <= 3f && newSpeed >= 0.25f) {
                        speedSelectionDialog.setCustomSpeed(newSpeed)
                    }
                    builtDialog.dismiss()
                }

                true
            }

            builtDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            builtDialog.setOnShowListener {_ -> editText.requestFocus()}

            builtDialog
        } ?: throw IllegalStateException("Activity cannot be null");
    }
}