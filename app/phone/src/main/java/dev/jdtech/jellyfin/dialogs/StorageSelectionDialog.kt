package dev.jdtech.jellyfin.dialogs

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.core.R as CoreR

fun getStorageSelectionDialog(
    context: Context,
    onItemSelected: (which: Int) -> Unit,
    onCancel: () -> Unit,
): AlertDialog {
    val locations = context.getExternalFilesDirs(null).mapNotNull {
        val locationStringRes = if (Environment.isExternalStorageRemovable(it)) CoreR.string.external else CoreR.string.internal
        val stat = StatFs(it.path)
        context.getString(CoreR.string.storage_name, context.getString(locationStringRes), stat.availableBytes.div(1000000))
    }.toTypedArray()
    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(CoreR.string.select_storage_location)
        .setItems(locations) { _, which ->
            onItemSelected(which)
        }
        .setOnCancelListener {
            onCancel()
        }.create()
    return dialog
}
