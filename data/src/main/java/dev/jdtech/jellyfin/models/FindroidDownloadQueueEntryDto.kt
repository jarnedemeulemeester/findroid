package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One item waiting in, or being worked on by, the download queue.
 *
 * Deliberately carries no foreign key to `episodes` or `movies`: a queued item has no row in either
 * table until its download actually starts, so a key would reject the very rows this table exists
 * to keep.
 *
 * Only durable facts live here. Progress and byte counts are left out on purpose — they change
 * every second, and DownloadManager is their real owner, so they are re-read on restore rather than
 * written back constantly.
 */
@Entity(tableName = "downloadQueue")
data class FindroidDownloadQueueEntryDto(
    @PrimaryKey val itemId: UUID,
    val batchId: UUID,
    /** Which lookup rebuilds the item on restore: an episode and a movie are fetched differently. */
    val itemType: String,
    val sourceId: String,
    val storageIndex: Int,
    /** [dev.jdtech.jellyfin.utils.DownloadEntryStatus] by name. */
    val status: String,
    val downloadId: Long?,
    val retries: Int,
    /** Insertion order, which is queue order. Episodes must resume in the order they were added. */
    val position: Long,
    /**
     * How many items the batch accepted when it was submitted. Stored per row rather than derived
     * from a count, so cancelling one episode does not quietly renumber "3 of 10" into "3 of 9".
     */
    val batchTotal: Int,
)
