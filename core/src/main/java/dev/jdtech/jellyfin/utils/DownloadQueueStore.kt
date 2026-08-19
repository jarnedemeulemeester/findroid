package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidDownloadQueueEntryDto
import dev.jdtech.jellyfin.models.FindroidItem
import java.util.UUID

/** What kind of item an entry holds, which decides how it is rebuilt after a restart. */
enum class QueuedItemType {
    MOVIE,
    EPISODE,
}

/**
 * A queue entry reduced to what is worth surviving a restart. Progress is absent on purpose:
 * DownloadManager owns it and it is re-read on restore, so it never has to be written back.
 */
data class PersistedQueueEntry(
    val itemId: UUID,
    val batchId: UUID,
    val itemType: QueuedItemType,
    val sourceId: String,
    val storageIndex: Int,
    val status: DownloadEntryStatus,
    val downloadId: Long?,
    val retries: Int,
    val position: Long,
    val batchTotal: Int,
)

/** Where the queue keeps itself so a restart does not lose the rest of a season. */
interface DownloadQueueStore {
    /** In queue order. */
    suspend fun load(): List<PersistedQueueEntry>

    suspend fun save(entry: PersistedQueueEntry)

    suspend fun remove(itemId: UUID)
}

/**
 * Rebuilds the item behind a persisted entry.
 *
 * The queue cannot hold onto the item across a restart: [Downloader.downloadItem] needs a real one,
 * with its media sources, series and season ids, trickplay and images, and a stub would silently
 * write half a download. So only the id and type are stored, and the item is fetched again.
 */
fun interface QueuedItemLoader {
    /** Null when the item can no longer be found, in which case the entry is dropped. */
    suspend fun load(itemId: UUID, type: QueuedItemType): FindroidItem?
}

class DatabaseDownloadQueueStore(private val database: ServerDatabaseDao) : DownloadQueueStore {
    override suspend fun load(): List<PersistedQueueEntry> =
        database.getDownloadQueue().mapNotNull { it.toPersistedOrNull() }

    override suspend fun save(entry: PersistedQueueEntry) {
        database.insertDownloadQueueEntry(
            FindroidDownloadQueueEntryDto(
                itemId = entry.itemId,
                batchId = entry.batchId,
                itemType = entry.itemType.name,
                sourceId = entry.sourceId,
                storageIndex = entry.storageIndex,
                status = entry.status.name,
                downloadId = entry.downloadId,
                retries = entry.retries,
                position = entry.position,
                batchTotal = entry.batchTotal,
            )
        )
    }

    override suspend fun remove(itemId: UUID) = database.deleteDownloadQueueEntry(itemId)

    /**
     * Null for a row this build cannot read, rather than a thrown exception: an enum constant
     * renamed or removed in a later version must not stop the whole queue from loading.
     */
    private fun FindroidDownloadQueueEntryDto.toPersistedOrNull(): PersistedQueueEntry? {
        val type = QueuedItemType.entries.firstOrNull { it.name == itemType } ?: return null
        val entryStatus = DownloadEntryStatus.entries.firstOrNull { it.name == status } ?: return null
        return PersistedQueueEntry(
            itemId = itemId,
            batchId = batchId,
            itemType = type,
            sourceId = sourceId,
            storageIndex = storageIndex,
            status = entryStatus,
            downloadId = downloadId,
            retries = retries,
            position = position,
            batchTotal = batchTotal,
        )
    }
}
