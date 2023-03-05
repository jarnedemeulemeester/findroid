package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: ServerDatabaseDao

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.DOWNLOAD_COMPLETE") {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != -1L) {
                val source = database.getSourceByDownloadId(id)
                if (source != null) {
                    val path = source.path.replace(".download", "")
                    val successfulRename = File(source.path).renameTo(File(path))
                    if (successfulRename) {
                        database.setSourcePath(source.id, path)
                    } else {
                        database.deleteSource(source.id)
                        // TODO can also be other type such as show, season or episode
                        database.deleteMovie(source.itemId)
                    }
                } else {
                    val mediaStream = database.getMediaStreamByDownloadId(id)
                    if (mediaStream != null) {
                        val path = mediaStream.path.replace(".download", "")
                        val successfulRename = File(mediaStream.path).renameTo(File(path))
                        if (successfulRename) {
                            database.setMediaStreamPath(mediaStream.id, path)
                        } else {
                            database.deleteMediaStream(mediaStream.id)
                        }
                    }
                }
            }
        }
    }
}
