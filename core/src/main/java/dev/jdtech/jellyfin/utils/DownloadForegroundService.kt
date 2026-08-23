package dev.jdtech.jellyfin.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.core.R as CoreR
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Keeps the process alive while the queue has work.
 *
 * Without it the system is free to freeze the process once the app leaves the screen. The download
 * in flight survives, since DownloadManager owns it, but nothing behind it ever starts.
 */
@AndroidEntryPoint
class DownloadForegroundService : Service() {
    @Inject lateinit var downloadQueue: DownloadQueue

    private val scope = SupervisorJob().let { CoroutineScope(it + Dispatchers.Main.immediate) }
    private var observer: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // First: the system kills a foreground service that has not shown a notification.
        startForegroundWith(getString(CoreR.string.download_pending))

        if (observer == null) {
            observer =
                scope.launch {
                    downloadQueue.state.collect { snapshot ->
                        val live = snapshot.entries.filterNot { it.status.isTerminal }
                        if (live.isEmpty()) {
                            // Nothing left to do, so stop holding a notification.
                            stopSelf()
                            return@collect
                        }
                        val current = live.firstOrNull { it.status != DownloadEntryStatus.QUEUED }
                        startForegroundWith(
                            text = current?.name ?: live.first().name,
                            progress = current?.progress ?: -1,
                            remaining = live.size,
                        )
                    }
                }
        }
        // Redeliver so the queue keeps being watched if the system restarts us.
        return START_STICKY
    }

    private fun startForegroundWith(text: String, progress: Int = -1, remaining: Int = 0) {
        val notification = buildNotification(text, progress, remaining)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
            }
        } catch (e: Exception) {
            // Refused outright in some states, such as a start from the background. Downloads
            // still run, only without the protection.
            Timber.e(e, "Could not start the download foreground service")
        }
    }

    private fun buildNotification(text: String, progress: Int, remaining: Int): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID,
                        getString(CoreR.string.download_notification_channel),
                        // Low: this is a progress report, not something to interrupt anyone for.
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply { setShowBadge(false) }
            )
        }
        val title =
            if (remaining > 1) {
                getString(CoreR.string.download_notification_title_many, remaining)
            } else {
                getString(CoreR.string.download_notification_title)
            }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(CoreR.drawable.ic_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                // No total size means no honest percentage to draw.
                if (progress in 0..100) setProgress(100, progress, false)
                else setProgress(0, 0, true)
            }
            .build()
    }

    override fun onDestroy() {
        observer?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 4815

        /** Safe to call repeatedly; the service ignores a start it is already serving. */
        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not start the download foreground service")
            }
        }
    }
}
