package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Strictly one at a time, which lets the dispatcher coroutine double as the progress
 * poller. Mirrored into [DownloadQueueStore] so a restart does not lose everything behind
 * the item in flight, and does not fetch a second copy of the item that was.
 */
class DownloadQueueImpl(
    private val downloader: Downloader,
    private val sources: DownloadedSources,
    private val store: DownloadQueueStore,
    private val loadItem: QueuedItemLoader,
    /** Read per submit, never cached: the toggle must take effect without an app restart. */
    private val isSequentialEnabled: () -> Boolean,
    /** Raised when work is accepted, so downloads survive the app leaving the screen. */
    private val onWorkAccepted: () -> Unit,
    private val scope: CoroutineScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, e -> Timber.e(e, "Download queue coroutine failed") }
        ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : DownloadQueue {

    private class Entry(
        val batchId: UUID,
        val item: FindroidItem,
        val itemType: QueuedItemType,
        val sourceId: String,
        val storageIndex: Int,
        /** Insertion order, held explicitly so queue order can be rebuilt from the database. */
        val position: Long,
        var status: DownloadEntryStatus = DownloadEntryStatus.QUEUED,
        var progress: Int = -1,
        var bytesDownloaded: Long = 0,
        var downloadId: Long? = null,
        var errorText: UiText? = null,
        var retries: Int = 0,
    ) {
        val itemId: UUID
            get() = item.id

        /**
         * The parts worth a database write. Progress changes every second and DownloadManager owns
         * it, so it is deliberately not in here.
         */
        val durable: Triple<DownloadEntryStatus, Long?, Int>
            get() = Triple(status, downloadId, retries)
    }

    private sealed interface Work {
        val entry: Entry

        /** Not started yet: hand it to [Downloader.downloadItem]. */
        data class Start(override val entry: Entry) : Work

        /** Already with DownloadManager, from before a restart: only watch it finish. */
        data class Resume(override val entry: Entry) : Work
    }

    private val mutex = Mutex()

    /** Insertion order IS queue order. Keyed by item id, which is also the dedupe key. */
    private val entries = LinkedHashMap<UUID, Entry>()

    private val batchTotals = LinkedHashMap<UUID, Int>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var pruneJob: Job? = null
    private var nextPosition = 0L

    private val _state = MutableStateFlow(DownloadQueueState())
    override val state = _state.asStateFlow()

    init {
        scope.launch {
            while (true) {
                wake.receive()
                // One thrown exception must not wedge the dispatcher for the rest of the process.
                try {
                    drain()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Download queue drain failed")
                }
            }
        }
    }

    override fun resume() {
        scope.launch {
            try {
                restore()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Could not restore the download queue")
            }
        }
    }

    /**
     * Rebuilds the queue from the database and reconciles it with what DownloadManager actually
     * has, since anything could have happened while the process was dead.
     */
    private suspend fun restore() {
        val persisted = withContext(ioDispatcher) { store.load() }
        if (persisted.isEmpty()) return

        // A batch with nothing left to do is history, and would otherwise reappear at every launch.
        val liveBatches =
            persisted.filterNot { it.status.isTerminal }.mapTo(mutableSetOf()) { it.batchId }
        val (keep, drop) = persisted.partition { it.batchId in liveBatches }
        forget(drop.map { it.itemId })

        val rebuilt =
            keep.mapNotNull { row ->
                val item = runCatching { loadItem.load(row.itemId, row.itemType) }.getOrNull()
                if (item == null) {
                    Timber.w("Dropping queued ${row.itemId}: its item could not be loaded")
                    forget(listOf(row.itemId))
                    null
                } else {
                    Entry(
                        batchId = row.batchId,
                        item = item,
                        itemType = row.itemType,
                        sourceId = row.sourceId,
                        storageIndex = row.storageIndex,
                        position = row.position,
                        status = row.status,
                        downloadId = row.downloadId,
                        retries = row.retries,
                    ) to row.batchTotal
                }
            }
        if (rebuilt.isEmpty()) return

        // Outside the lock: reconciling asks DownloadManager and the database about each entry.
        rebuilt.forEach { (entry, _) -> reconcile(entry) }

        val adopted = mutableListOf<Entry>()
        mutex.withLock {
            for ((entry, batchTotal) in rebuilt) {
                // Anything submitted since startup wins; its download may already be running.
                if (entries.containsKey(entry.itemId)) continue
                entries[entry.itemId] = entry
                batchTotals[entry.batchId] = batchTotal
                adopted += entry
            }
            // From every restored row, not just the adopted ones: a reused position would put a new
            // item ahead of one already waiting.
            nextPosition = maxOf(nextPosition, rebuilt.maxOf { it.first.position } + 1)
            if (adopted.isEmpty()) return@withLock
            publishLocked()
            adopted.forEach { rememberLocked(it) }
        }

        if (adopted.any { !it.status.isTerminal }) {
            onWorkAccepted()
            wake.trySend(Unit)
        }
    }

    /** Works out what really became of a restored entry while the process was gone. */
    private suspend fun reconcile(entry: Entry) {
        if (entry.status.isTerminal || entry.status == DownloadEntryStatus.QUEUED) return

        // PREPARING means the process died inside downloadItem, which enqueues before it writes the
        // sources row. That row is the only record of whether it got that far.
        val downloadId =
            entry.downloadId
                ?: withContext(ioDispatcher) { sources.downloadIdFor(entry.itemId, entry.sourceId) }
        val info = downloadId?.let { downloader.getDownloadInfo(it) }

        when {
            info != null && !info.status.isFinishedDownload -> {
                entry.downloadId = downloadId
                entry.status = DownloadEntryStatus.RUNNING
                entry.bytesDownloaded = info.bytesDownloaded
            }
            isAlreadyDownloaded(entry) -> {
                entry.status = DownloadEntryStatus.SUCCEEDED
                entry.progress = 100
            }
            else -> {
                // Nothing to adopt and nothing finished on disk, so start it over. A partial file is no
                // help: the server may not support ranges, and DownloadManager has forgotten the job.
                entry.downloadId = null
                entry.progress = -1
                entry.bytesDownloaded = 0
                entry.status = DownloadEntryStatus.QUEUED
            }
        }
    }

    override suspend fun submit(requests: List<DownloadRequest>): SubmitOutcome {
        if (!isSequentialEnabled()) {
            return SubmitOutcome.Bypassed
        }
        val batchId = UUID.randomUUID()
        var accepted = 0
        val outcome =
            mutex.withLock {
                pruneJob?.cancel()
                forget(pruneTerminalBatchesLocked())
                val added = mutableListOf<Entry>()
                for (request in requests) {
                    val existing = entries[request.item.id]
                    if (existing != null && !existing.status.isTerminal) continue
                    // put() on an existing key keeps its original position, which is what the
                    // retry path wants.
                    val entry =
                        Entry(
                            batchId = batchId,
                            item = request.item,
                            itemType = request.item.queuedItemType,
                            sourceId = request.sourceId,
                            storageIndex = request.storageIndex,
                            position = nextPosition++,
                        )
                    entries[request.item.id] = entry
                    added += entry
                    accepted++
                }
                val result =
                    if (accepted > 0) {
                        batchTotals[batchId] = accepted
                        SubmitOutcome.Queued(batchId, accepted)
                    } else {
                        // Everything asked for is already live. A batch id with no entries would never gain any,
                        // so hand back the batch that actually owns the work.
                        val owner = requests.firstNotNullOfOrNull { entries[it.item.id] }
                        if (owner != null) {
                            SubmitOutcome.Queued(
                                batchId = owner.batchId,
                                accepted = batchTotals[owner.batchId] ?: 1,
                                isNewBatch = false,
                            )
                        } else {
                            SubmitOutcome.Bypassed
                        }
                    }
                publishLocked()
                added.forEach { rememberLocked(it) }
                result
            }
        if (outcome is SubmitOutcome.Queued) {
            // Downloads must keep going once the app is off screen, or the queue stops advancing.
            onWorkAccepted()
        }
        wake.trySend(Unit)
        return outcome
    }

    override suspend fun cancel(itemIds: Set<UUID>) {
        // Remove under the lock first, so nextWorkLocked cannot pick a victim and awaitTerminal
        // sees the mismatch and bails out.
        val victims =
            mutex.withLock {
                val removed = itemIds.mapNotNull { entries.remove(it) }
                if (removed.isNotEmpty()) {
                    dropOrphanedBatchTotalsLocked()
                    publishLocked()
                }
                forget(removed.map { it.itemId })
                removed.filterNot { it.status == DownloadEntryStatus.SUCCEEDED }
            }
        // On the queue's scope, not the caller's: the view model asking for the cancel is usually
        // about to be cleared.
        scope.launch { removeVictims(victims) }
    }

    override suspend fun cancelBatch(batchId: UUID) {
        val victims =
            mutex.withLock {
                val removed = entries.values.filter { it.batchId == batchId }
                removed.forEach { entries.remove(it.itemId) }
                batchTotals.remove(batchId)
                if (removed.isNotEmpty()) publishLocked()
                forget(removed.map { it.itemId })
                // A succeeded entry's row still carries its download id, so cancelDownload would find the
                // finished file and delete it. Cancel means stop, not delete what already finished.
                removed.filterNot { it.status == DownloadEntryStatus.SUCCEEDED }
            }
        scope.launch { removeVictims(victims) }
    }

    /**
     * Deliberately does not cancel a victim's prepare job. [Downloader.downloadItem] enqueues
     * with DownloadManager before it writes the row carrying the download id, so cancelling in
     * that window strands a full size file nothing can reach. [prepare] cleans up its own
     * orphan instead.
     */
    private suspend fun removeVictims(victims: List<Entry>) {
        for (victim in victims) {
            // QUEUED never reached downloadItem, so there is nothing of ours to cancel, and looking
            // its id up could match a row left by an unrelated finished download. PREPARING is still
            // inside downloadItem and cleans up after itself.
            if (
                victim.status == DownloadEntryStatus.QUEUED ||
                    victim.status == DownloadEntryStatus.PREPARING
            ) {
                continue
            }
            // Prefer the id the queue knows, falling back to the sources row downloadItem wrote.
            val downloadId =
                victim.downloadId
                    ?: withContext(ioDispatcher) {
                        sources.downloadIdFor(victim.itemId, victim.sourceId)
                    }
            if (downloadId != null) {
                try {
                    downloader.cancelDownload(victim.item, downloadId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to cancel download")
                }
            }
        }
        wake.trySend(Unit)
    }

    private suspend fun drain() {
        while (true) {
            val work =
                mutex.withLock { nextWorkLocked() }
                    ?: run {
                        scheduleIdlePrune()
                        return
                    }
            val entry = work.entry
            try {
                if (work is Work.Start) {
                    // Run prepare in its own child so a failure inside it cannot take the
                    // dispatcher down with it; join() does not rethrow the child's failure.
                    scope.launch { prepare(entry) }.join()
                }
                awaitTerminal(entry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Download queue entry failed")
            } finally {
                // An entry left non terminal would be handed back forever and stall the queue.
                mutex.withLock {
                    // QUEUED is deliberate: a retry puts the entry back in line, and failing it here would mean
                    // the retry never happened.
                    if (
                        entries[entry.itemId] === entry &&
                            !entry.status.isTerminal &&
                            entry.status != DownloadEntryStatus.QUEUED
                    ) {
                        entry.status = DownloadEntryStatus.FAILED
                        if (entry.errorText == null) {
                            entry.errorText = UiText.StringResource(CoreR.string.download_failed)
                        }
                        publishLocked()
                        rememberLocked(entry)
                    }
                }
            }
        }
    }

    /** Flips the chosen entry to PREPARING so it cannot be picked twice. */
    private fun nextWorkLocked(): Work? {
        val busy =
            entries.values.firstOrNull {
                !it.status.isTerminal && it.status != DownloadEntryStatus.QUEUED
            }
        if (busy != null) {
            // Only reachable for an entry restored from a previous run. It is already with
            // DownloadManager, so it must be watched rather than enqueued again.
            return busy.downloadId?.let { Work.Resume(busy) }
        }
        return entries.values
            .firstOrNull { it.status == DownloadEntryStatus.QUEUED }
            ?.also {
                it.status = DownloadEntryStatus.PREPARING
                publishLocked()
            }
            ?.let { Work.Start(it) }
    }

    private suspend fun prepare(entry: Entry) {
        // Re-resolve at dequeue time: the caller's snapshot may be minutes old by now.
        if (isAlreadyDownloaded(entry)) {
            mutex.withLock {
                if (entries[entry.itemId] === entry) {
                    entry.status = DownloadEntryStatus.SUCCEEDED
                    entry.progress = 100
                    publishLocked()
                    rememberLocked(entry)
                }
            }
            return
        }

        // A download for this source may already be in flight, left by a queue that died before it
        // was persisted. Adopting it is what prevents a second copy of the same file.
        val inFlight =
            withContext(ioDispatcher) { sources.downloadIdFor(entry.itemId, entry.sourceId) }
        val inFlightStatus = inFlight?.let { downloader.getDownloadInfo(it)?.status }
        if (inFlight != null && inFlightStatus != null && !inFlightStatus.isFinishedDownload) {
            mutex.withLock {
                if (entries[entry.itemId] === entry) {
                    entry.downloadId = inFlight
                    entry.status = DownloadEntryStatus.RUNNING
                    publishLocked()
                    rememberLocked(entry)
                }
            }
            Timber.d("Adopted the download already running for ${entry.item.name}")
            return
        }

        val result =
            try {
                downloader.downloadItem(entry.item, entry.sourceId, entry.storageIndex)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                Pair(-1L, UiText.StringResource(CoreR.string.unknown_error))
            }

        val orphanId =
            mutex.withLock {
                if (entries[entry.itemId] !== entry) {
                    // Cancelled while preparing. If the enqueue already happened, clean it up.
                    result.first.takeIf { it != -1L }
                } else {
                    if (result.first == -1L) {
                        entry.status = DownloadEntryStatus.FAILED
                        entry.errorText =
                            result.second ?: UiText.StringResource(CoreR.string.unknown_error)
                    } else {
                        entry.downloadId = result.first
                        entry.status = DownloadEntryStatus.RUNNING
                    }
                    publishLocked()
                    rememberLocked(entry)
                    null
                }
            }
        if (orphanId != null) {
            try {
                downloader.cancelDownload(entry.item, orphanId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to clean up orphaned download")
            }
        }
    }

    private suspend fun awaitTerminal(entry: Entry) {
        while (true) {
            val downloadId =
                mutex.withLock {
                    when {
                        entries[entry.itemId] !== entry -> null
                        entry.status.isTerminal -> null
                        else -> entry.downloadId
                    }
                } ?: return

            val info = downloader.getDownloadInfo(downloadId)
            // A null row means DownloadManager has forgotten the id, and only the database can say
            // whether that was success or a lost job.
            val renamed = if (info == null) isAlreadyDownloaded(entry) else false

            val terminal =
                mutex.withLock {
                    if (entries[entry.itemId] !== entry) {
                        true
                    } else {
                        val before = entry.durable
                        applyInfoLocked(entry, info, renamed)
                        publishLocked()
                        // Only when something durable changed; applyInfoLocked runs every poll.
                        if (before != entry.durable) rememberLocked(entry)
                        entry.status.isTerminal
                    }
                }
            if (terminal) return

            delay(pollIntervalMs)
        }
    }

    private suspend fun isAlreadyDownloaded(entry: Entry): Boolean =
        withContext(ioDispatcher) { sources.isDownloaded(entry.itemId, entry.sourceId) }

    private fun applyInfoLocked(entry: Entry, info: DownloadInfo?, renamed: Boolean) {
        when {
            info == null && renamed -> {
                entry.status = DownloadEntryStatus.SUCCEEDED
                entry.progress = 100
            }
            info == null -> {
                entry.status = DownloadEntryStatus.FAILED
                entry.errorText = UiText.StringResource(CoreR.string.download_failed)
            }
            info.status == DownloadManager.STATUS_SUCCESSFUL -> {
                entry.status = DownloadEntryStatus.SUCCEEDED
                entry.progress = 100
            }
            info.status == DownloadManager.STATUS_FAILED -> {
                if (entry.retries < MAX_RETRIES && isWorthRetrying(info.reason)) {
                    // A server without range support cannot be resumed, so DownloadManager reports failure
                    // rather than continuing. Starting over is the only way to finish it.
                    entry.retries++
                    entry.downloadId = null
                    entry.progress = -1
                    // A restart begins from zero; keeping the old count would run progress backwards.
                    entry.bytesDownloaded = 0
                    entry.status = DownloadEntryStatus.QUEUED
                    Timber.d("Retrying ${entry.item.name}, attempt ${entry.retries}")
                } else {
                    entry.status = DownloadEntryStatus.FAILED
                    entry.errorText = UiText.StringResource(CoreR.string.download_failed)
                }
            }
            info.status == DownloadManager.STATUS_PAUSED -> {
                entry.status = DownloadEntryStatus.PAUSED
                if (info.progress >= 0) entry.progress = info.progress
                entry.bytesDownloaded = info.bytesDownloaded
            }
            else -> {
                entry.status = DownloadEntryStatus.RUNNING
                if (info.progress >= 0) entry.progress = info.progress
                entry.bytesDownloaded = info.bytesDownloaded
            }
        }
    }

    /**
     * Drops finished entries once the queue has been idle, so a finished batch does not pin its
     * items for the rest of the process. The delay matters: clearing the moment the last entry
     * goes terminal lets StateFlow conflation swallow the snapshot that says so.
     */
    private fun scheduleIdlePrune() {
        pruneJob?.cancel()
        pruneJob =
            scope.launch {
                delay(IDLE_PRUNE_DELAY_MS)
                mutex.withLock {
                    if (entries.isNotEmpty() && entries.values.all { it.status.isTerminal }) {
                        val ids = entries.keys.toList()
                        entries.clear()
                        batchTotals.clear()
                        publishLocked()
                        forget(ids)
                    }
                }
            }
    }

    /** Drops totals for batches that no longer have entries. */
    private fun dropOrphanedBatchTotalsLocked() {
        val liveBatches = entries.values.mapTo(mutableSetOf()) { it.batchId }
        batchTotals.keys.retainAll(liveBatches)
    }

    /** Drops batches whose entries have all reached a terminal status. */
    private fun pruneTerminalBatchesLocked(): List<UUID> {
        val liveBatches =
            entries.values.filter { !it.status.isTerminal }.mapTo(mutableSetOf()) { it.batchId }
        val dropped = entries.values.filter { it.batchId !in liveBatches }.map { it.itemId }
        entries.values.removeAll { it.batchId !in liveBatches }
        batchTotals.keys.retainAll(liveBatches)
        return dropped
    }

    /** Losing the mirror costs resumability, not the download, so this never takes the queue down. */
    private suspend fun rememberLocked(entry: Entry) {
        try {
            withContext(ioDispatcher) {
                store.save(
                    PersistedQueueEntry(
                        itemId = entry.itemId,
                        batchId = entry.batchId,
                        itemType = entry.itemType,
                        sourceId = entry.sourceId,
                        storageIndex = entry.storageIndex,
                        status = entry.status,
                        downloadId = entry.downloadId,
                        retries = entry.retries,
                        position = entry.position,
                        batchTotal = batchTotals[entry.batchId] ?: 1,
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Could not persist a download queue entry")
        }
    }

    private suspend fun forget(itemIds: Collection<UUID>) {
        if (itemIds.isEmpty()) return
        try {
            withContext(ioDispatcher) { itemIds.forEach { store.remove(it) } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Could not remove download queue entries")
        }
    }

    private fun publishLocked() {
        _state.value =
            DownloadQueueState(
                entries =
                    entries.values.map { entry ->
                        DownloadEntry(
                            batchId = entry.batchId,
                            itemId = entry.itemId,
                            sourceId = entry.sourceId,
                            name = entry.item.name,
                            status = entry.status,
                            progress = entry.progress,
                            bytesDownloaded = entry.bytesDownloaded,
                            downloadId = entry.downloadId,
                            errorText = entry.errorText,
                        )
                    },
                batchTotals = batchTotals.toMap(),
            )
    }

    /**
     * Whether starting over could fix the failure. A dropped or truncated connection could;
     * running out of room, or a destination that has gone away, could not.
     */
    private fun isWorthRetrying(reason: Int): Boolean =
        reason == DownloadManager.ERROR_CANNOT_RESUME ||
            reason == DownloadManager.ERROR_HTTP_DATA_ERROR ||
            reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS ||
            reason == DownloadManager.ERROR_UNKNOWN ||
            reason == 0

    companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 1_000L
        const val MAX_RETRIES = 2
        const val IDLE_PRUNE_DELAY_MS = 30_000L
    }
}

private val Int.isFinishedDownload: Boolean
    get() = this == DownloadManager.STATUS_SUCCESSFUL || this == DownloadManager.STATUS_FAILED

private val FindroidItem.queuedItemType: QueuedItemType
    get() = if (this is FindroidEpisode) QueuedItemType.EPISODE else QueuedItemType.MOVIE
