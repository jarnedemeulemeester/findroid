package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.IllegalStateException
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

class VideoVersionDialogFragment(
    private val item: BaseItemDto,
    private val viewModel: PlayerViewModel
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = item.mediaSources?.map { it.name }?.toTypedArray()
        return activity?.let { activity ->
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.select_a_version)
                .setItems(items) { _, which ->
                    viewModel.loadPlayerItems(item, which)
                }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}