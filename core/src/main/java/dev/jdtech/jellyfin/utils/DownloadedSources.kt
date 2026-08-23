package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.util.UUID

/** The two facts the queue needs from the database, kept narrow so it can be tested without one. */
interface DownloadedSources {
    suspend fun isDownloaded(itemId: UUID, sourceId: String): Boolean

    /** Recovers the id of a download enqueued before the queue learned it, so it can be cancelled. */
    suspend fun downloadIdFor(itemId: UUID, sourceId: String): Long?
}

class DatabaseDownloadedSources(private val database: ServerDatabaseDao) : DownloadedSources {
    // A path still ending in .download is a file DownloadReceiver has not finished with.
    override suspend fun isDownloaded(itemId: UUID, sourceId: String): Boolean =
        database.getSources(itemId).any { it.id == sourceId && !it.path.endsWith(".download") }

    override suspend fun downloadIdFor(itemId: UUID, sourceId: String): Long? =
        database.getSources(itemId).firstOrNull { it.id == sourceId }?.downloadId
}
