package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One item waiting in, or being worked on by, the download queue.
 *
 * No foreign key to `episodes` or `movies` on purpose: a queued item has no row in either until its
 * download starts, so a key would reject the very rows this table exists to keep. Progress is left
 * out for a similar reason, it changes every second and DownloadManager owns it.
 */
@Entity(tableName = "downloadQueue")
data class FindroidDownloadQueueEntryDto(
    @PrimaryKey val itemId: UUID,
    val batchId: UUID,
    /** Which lookup rebuilds the item on restore, since episodes and movies are fetched apart. */
    val itemType: String,
    val sourceId: String,
    val storageIndex: Int,
    /** [dev.jdtech.jellyfin.utils.DownloadEntryStatus] by name. */
    val status: String,
    val downloadId: Long?,
    val retries: Int,
    /** Insertion order, which is queue order. */
    val position: Long,
    /** Stored rather than counted, so cancelling one episode does not renumber "3 of 10". */
    val batchTotal: Int,
)
