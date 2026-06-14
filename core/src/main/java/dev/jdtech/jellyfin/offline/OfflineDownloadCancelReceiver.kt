package dev.jdtech.jellyfin.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OfflineDownloadCancelReceiver : BroadcastReceiver() {
    @Inject lateinit var offlineDownloadManager: OfflineDownloadManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_PACKAGE) return
        val packageId = intent.getStringExtra(EXTRA_PACKAGE_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                offlineDownloadManager.cancelVideoPackage(packageId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_PACKAGE = "dev.jdtech.jellyfin.offline.CANCEL_PACKAGE"
        private const val EXTRA_PACKAGE_ID = "packageId"

        fun intent(context: Context, packageId: String): Intent =
            Intent(context, OfflineDownloadCancelReceiver::class.java)
                .setAction(ACTION_CANCEL_PACKAGE)
                .putExtra(EXTRA_PACKAGE_ID, packageId)
    }
}
