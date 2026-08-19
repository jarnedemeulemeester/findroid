package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.util.UUID

/**
 * The two facts the download queue needs from the database, kept separate from the 78-method DAO so
 * the queue can be exercised without one.
 */
interface DownloadedSources {
    /** Whether the file for [sourceId] is on disk and finished. */
    suspend fun isDownloaded(itemId: UUID, sourceId: String): Boolean

    /**
     * The DownloadManager id recorded for [sourceId], if one was. Recovers the id for a download
     * that was cancelled after being enqueued but before the queue learned its id.
     */
    suspend fun downloadIdFor(itemId: UUID, sourceId: String): Long?
}

class DatabaseDownloadedSources(private val database: ServerDatabaseDao) : DownloadedSources {
    // A path still ending in .download is a file DownloadReceiver has not finished with.
    override suspend fun isDownloaded(itemId: UUID, sourceId: String): Boolean =
        database.getSources(itemId).any { it.id == sourceId && !it.path.endsWith(".download") }

    override suspend fun downloadIdFor(itemId: UUID, sourceId: String): Long? =
        database.getSources(itemId).firstOrNull { it.id == sourceId }?.downloadId
}
