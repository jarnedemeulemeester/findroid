package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow

/** One item the queue has been asked to fetch. */
data class DownloadRequest(
    val item: FindroidItem,
    val sourceId: String,
    val storageIndex: Int = 0,
)

enum class DownloadEntryStatus {
    /** Accepted, not yet handed to [Downloader.downloadItem]. */
    QUEUED,

    /** Inside [Downloader.downloadItem]: media source lookup, DB writes, trickplay tiles. */
    PREPARING,
    RUNNING,
    PAUSED,
    SUCCEEDED,
    FAILED;

    val isTerminal: Boolean
        get() = this == SUCCEEDED || this == FAILED
}

data class DownloadEntry(
    val batchId: UUID,
    val itemId: UUID,
    val sourceId: String,
    val name: String,
    val status: DownloadEntryStatus,
    /** 0..100, or -1 while the total size is still unknown. */
    val progress: Int = -1,
    /** Bytes on disk so far; the only honest progress signal when [progress] is -1. */
    val bytesDownloaded: Long = 0,
    val downloadId: Long? = null,
    val errorText: UiText? = null,
)

data class DownloadQueueState(
    val entries: List<DownloadEntry> = emptyList(),
    /** Accepted item count per batch, so a shrinking batch still renders "n of total". */
    val batchTotals: Map<UUID, Int> = emptyMap(),
)

sealed interface SubmitOutcome {
    /**
     * The sequential downloads preference is off. Nothing was queued and the queue was not touched
     * at all; the caller downloads directly, exactly as it does today.
     */
    data object Bypassed : SubmitOutcome

    /**
     * [isNewBatch] is false when every request was already live in an earlier batch and
     * [batchId] therefore identifies that pre-existing batch rather than one this call created.
     * Callers must not assume ownership of a batch they did not create.
     */
    data class Queued(val batchId: UUID, val accepted: Int, val isNewBatch: Boolean = true) :
        SubmitOutcome
}

/**
 * App scoped, strictly one at a time download queue. Only ever used when the sequential downloads
 * preference is enabled — see [submit].
 *
 * Serialization has to happen here rather than in the system DownloadManager: DownloadManager runs
 * everything it has been handed concurrently, so the only way to give one item the full bandwidth
 * is to delay the `enqueue` call for item N+1 until item N reaches a terminal status.
 */
interface DownloadQueue {
    val state: StateFlow<DownloadQueueState>

    /**
     * Rebuilds the queue saved by a previous run and picks the work back up. Call once at startup.
     * Without it the rest of a season is lost whenever the process dies, and re-adding it fetches a
     * second copy of whatever was already in flight.
     */
    fun resume()

    /**
     * Reads the sequential downloads preference at call time. When it is off this returns
     * [SubmitOutcome.Bypassed] and does nothing else. Otherwise [requests] are appended in the
     * given order as one batch; items already live in the queue are skipped.
     */
    suspend fun submit(requests: List<DownloadRequest>): SubmitOutcome

    /** Removes [itemIds] from the queue, cancelling any in flight DownloadManager job. */
    suspend fun cancel(itemIds: Set<UUID>)

    /** Removes every entry belonging to [batchId]. */
    suspend fun cancelBatch(batchId: UUID)
}
