package dev.jdtech.jellyfin.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.models.JellyfinItem
import dev.jdtech.jellyfin.player.video.R

fun getVideoVersionDialog(
    context: Context,
    item: JellyfinItem,
    onItemSelected: (which: Int) -> Unit
): AlertDialog {
    val items = item.sources.map { "${it.name} - ${it.type}" }.toTypedArray()
    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.select_a_version)
        .setItems(items) { _, which ->
            onItemSelected(which)
        }.create()
    return dialog
}
