package dev.jdtech.jellyfin.utils

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun Modifier.copyOnLongClick(text: String, onClick: (() -> Unit)? = null): Modifier {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    return this.combinedClickable(
        onClick = onClick ?: {},
        onLongClickLabel = "Copy text",
        onLongClick = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Copied text", text)))
            }
            // Only show a toast for Android 12 and lower.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    )
}