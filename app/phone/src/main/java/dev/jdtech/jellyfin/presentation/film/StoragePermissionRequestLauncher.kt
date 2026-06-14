package dev.jdtech.jellyfin.presentation.film

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderEvent

@Composable
fun rememberStoragePermissionRequestLauncher(onResult: () -> Unit): (DownloaderEvent.StoragePermissionRequired) -> Unit {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onResult()
        }
    return remember(launcher, onResult) {
        { event ->
            if (!launcher.launchSafely(event.intent)) {
                launcher.launchSafely(event.fallbackIntent)
            }
        }
    }
}

private fun androidx.activity.result.ActivityResultLauncher<Intent>.launchSafely(
    intent: Intent
): Boolean =
    try {
        launch(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
