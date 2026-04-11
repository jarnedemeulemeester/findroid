package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Clean up any files from before the mpv config and cache directories were changed.
 */
@HiltWorker
class MpvCleanupWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        // Delete the old mpv config directory.
        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir != null) {
            val oldConfigDir = File(externalFilesDir, "mpv")
            if (oldConfigDir.exists()) {
                oldConfigDir.deleteRecursively()
            }
        }

        // It may be possible that context.filesDir was used instead of the external dir.
        // Delete specific files from that directory.
        val oldConfigDir = File(context.filesDir, "mpv")
        if (oldConfigDir.exists()) {
            File(oldConfigDir, "subfont.ttf").delete()
            oldConfigDir.listFiles { _, name ->
                name.startsWith("shader_")
            }?.forEach {
                it.delete()
            }
        }

        return Result.success()
    }
}
