package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidSourceDto
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidMovie
import dev.jdtech.jellyfin.models.toFindroidSource
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject lateinit var database: ServerDatabaseDao

    @Inject lateinit var downloader: Downloader

    @Inject lateinit var repository: JellyfinRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.DOWNLOAD_COMPLETE") {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != -1L) {
                val downloadManager = context.getSystemService(DownloadManager::class.java)
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)
                val downloadStatus = if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                } else {
                    DownloadManager.STATUS_FAILED
                }
                val downloadReason = if (cursor.moveToFirst()) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                } else {
                    -1
                }
                cursor.close()

                val source = database.getSourceByDownloadId(id)
                if (source != null) {
                    if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                        val path = source.path.replace(".download", "")
                        if (renameFile(source.path, path)) {
                            database.setSourcePath(source.id, path)
                            Timber.d("Download complete, file at: $path")
                        } else {
                            Timber.e("Failed to rename download, deleting item. path=${source.path}")
                            deleteItemAsync(source, goAsync())
                        }
                    } else {
                        Timber.e("Download failed status=$downloadStatus reason=$downloadReason path=${source.path}")
                        deleteItemAsync(source, goAsync())
                    }
                } else {
                    val mediaStream = database.getMediaStreamByDownloadId(id)
                    if (mediaStream != null) {
                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            val path = mediaStream.path.replace(".download", "")
                            if (renameFile(mediaStream.path, path)) {
                                database.setMediaStreamPath(mediaStream.id, path)
                            } else {
                                Timber.e("Failed to rename media stream download, deleting. path=${mediaStream.path}")
                                database.deleteMediaStream(mediaStream.id)
                            }
                        } else {
                            Timber.e("Media stream download failed status=$downloadStatus reason=$downloadReason")
                            database.deleteMediaStream(mediaStream.id)
                        }
                    }
                }
            }
        }
    }

    private fun renameFile(fromPath: String, toPath: String): Boolean {
        val src = File(fromPath)
        val dst = File(toPath)
        if (src.renameTo(dst)) return true
        // renameTo can fail on some external storage filesystems (e.g. FAT32/exFAT on SD cards).
        // Fall back to copy + delete.
        return try {
            src.copyTo(dst, overwrite = true)
            src.delete()
            true
        } catch (e: Exception) {
            Timber.e(e, "copyTo fallback also failed: $fromPath -> $toPath")
            false
        }
    }

    private fun deleteItemAsync(sourceDto: FindroidSourceDto, pendingResult: PendingResult) {
        val source = sourceDto.toFindroidSource(database)
        val items = mutableListOf<FindroidItem>()
        items.addAll(database.getMovies().map { it.toFindroidMovie(database, repository.getUserId()) })
        items.addAll(database.getEpisodes().map { it.toFindroidEpisode(database, repository.getUserId()) })
        val item = items.firstOrNull { it.id == sourceDto.itemId }
        if (item == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                downloader.deleteItem(item, source)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
