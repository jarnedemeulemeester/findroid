package dev.jdtech.jellyfin.ui.dialogs

import androidx.compose.ui.window.DialogProperties
import com.ramcosta.composedestinations.spec.DestinationStyle

object BaseDialogStyle : DestinationStyle.Dialog() {
    override val properties = DialogProperties(
        dismissOnClickOutside = false,
        dismissOnBackPress = true,
        usePlatformDefaultWidth = false,
    )
}
