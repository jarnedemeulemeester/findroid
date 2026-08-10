package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.io.File

/**
 * Clean up any files from before the mpv config and cache directories were changed.
 */
@HiltWorker
class MpvCleanupWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    val appPreferences: AppPreferences,
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        migratePreference()
        cleanUpDirs()

        appPreferences.setValue(appPreferences.mpvMigrated, true)

        return Result.success()
    }

    private fun migratePreference() {
        // Migrate to new player backend preference
        val defaultMpv = appPreferences.getValue(appPreferences.playerMpv)
        if (defaultMpv) {
            appPreferences.setValue(appPreferences.playerBackend, "mpv")
        }
    }

    private fun cleanUpDirs() {
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
    }
}
