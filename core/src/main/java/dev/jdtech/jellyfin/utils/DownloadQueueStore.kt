package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidDownloadQueueEntryDto
import dev.jdtech.jellyfin.models.FindroidItem
import java.util.UUID

/** Decides how an entry is rebuilt after a restart. */
enum class QueuedItemType {
    MOVIE,
    EPISODE,
}

/** A queue entry reduced to what is worth surviving a restart. */
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

/** Where the queue keeps itself between runs. */
interface DownloadQueueStore {
    /** In queue order. */
    suspend fun load(): List<PersistedQueueEntry>

    suspend fun save(entry: PersistedQueueEntry)

    suspend fun remove(itemId: UUID)
}

/**
 * Rebuilds the item behind a persisted entry. Only its id and type are stored, because
 * [Downloader.downloadItem] needs a real item and a stub would write half a download.
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

    /** Null rather than throwing, so one unreadable row cannot stop the queue loading. */
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
