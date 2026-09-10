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

    /** Inside [Downloader.downloadItem], which is not instant: it also writes rows and images. */
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
    /** 0..100, or -1 when the total size is unknown. */
    val progress: Int = -1,
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
    /** The preference is off. Nothing was queued; the caller downloads directly as before. */
    data object Bypassed : SubmitOutcome

    /**
     * [isNewBatch] is false when every request was already live, so [batchId] identifies the batch
     * that already owns them rather than one this call created.
     */
    data class Queued(val batchId: UUID, val accepted: Int, val isNewBatch: Boolean = true) :
        SubmitOutcome
}

/**
 * App scoped, strictly one at a time download queue, used only when the sequential preference is on.
 *
 * Serialising has to happen here rather than in DownloadManager, which runs everything it is handed
 * at once: the only way to give one item the full bandwidth is to hold back the next enqueue until
 * the current item reaches a terminal status.
 */
interface DownloadQueue {
    val state: StateFlow<DownloadQueueState>

    /**
     * Rebuilds the queue saved by a previous run. Call once at startup: without it everything past
     * the item in flight is lost when the process dies.
     */
    fun resume()

    /**
     * Reads the preference at call time, returning [SubmitOutcome.Bypassed] when it is off.
     * Otherwise [requests] are appended in order as one batch, skipping items already queued.
     */
    suspend fun submit(requests: List<DownloadRequest>): SubmitOutcome

    /** Removes [itemIds] from the queue, cancelling any in flight DownloadManager job. */
    suspend fun cancel(itemIds: Set<UUID>)

    /** Removes every entry belonging to [batchId]. */
    suspend fun cancelBatch(batchId: UUID)
}
