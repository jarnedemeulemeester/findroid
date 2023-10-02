package dev.jdtech.jellyfin.utils

import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel

fun playerErrorDialogSnackbar(fragmentManager: FragmentManager, root: View, error: PlayerViewModel.PlayerItemError) {
    Snackbar.make(root, R.string.error_preparing_player_items, Snackbar.LENGTH_SHORT)
        .setAction(R.string.view_details) {
            ErrorDialogFragment.newInstance(error.error).show(fragmentManager, ErrorDialogFragment.TAG)
        }
        .show()
}
