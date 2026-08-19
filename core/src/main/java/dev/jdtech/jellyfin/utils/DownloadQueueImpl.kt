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
 * Because the sequential preference being off bypasses the queue entirely (see
 * [DownloadQueue.submit]), the queue is unconditionally sequential: at most one download is ever in
 * flight, which means the dispatcher coroutine can double as the progress poller.
 *
 * The queue is mirrored into [DownloadQueueStore] so it survives the process dying. Without that a
 * season download lost everything past the episode in flight, and re-adding it fetched a second
 * copy of that episode, because nothing was left that remembered the first.
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

    /** What the dispatcher should do with an entry it has picked up. */
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
                // Per iteration containment: one thrown exception must never wedge the dispatcher
                // for the rest of the process lifetime.
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

        // A batch with nothing left to do is history. Keeping it would put a finished season's
        // card back on screen at every launch.
        val liveBatches =
            persisted.filterNot { it.status.isTerminal }.mapTo(mutableSetOf()) { it.batchId }
        val (keep, drop) = persisted.partition { it.batchId in liveBatches }
        forget(drop.map { it.itemId })

        // Rehydrating goes to the repository, so it happens before the lock is taken.
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
                // Anything submitted since the process came up wins: it is newer, and its download
                // may already be running.
                if (entries.containsKey(entry.itemId)) continue
                entries[entry.itemId] = entry
                batchTotals[entry.batchId] = batchTotal
                adopted += entry
            }
            // Taken from every restored row rather than only the adopted ones: handing the same
            // position out twice would put a newly submitted item ahead of one already waiting.
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

        // PREPARING means the process died inside downloadItem, which enqueues with DownloadManager
        // before it writes the sources row. That row is the only record of whether it got that far.
        val downloadId =
            entry.downloadId
                ?: withContext(ioDispatcher) { sources.downloadIdFor(entry.itemId, entry.sourceId) }
        val info = downloadId?.let { downloader.getDownloadInfo(it) }

        when {
            info != null && !info.status.isFinishedDownload -> {
                // Still going, or paused: adopt it and let the dispatcher watch it.
                entry.downloadId = downloadId
                entry.status = DownloadEntryStatus.RUNNING
                entry.bytesDownloaded = info.bytesDownloaded
            }
            isAlreadyDownloaded(entry) -> {
                entry.status = DownloadEntryStatus.SUCCEEDED
                entry.progress = 100
            }
            else -> {
                // Nothing to adopt and nothing finished on disk, so start it over. A transcode
                // cannot be resumed anyway, so there is nothing in a partial file worth keeping.
                entry.downloadId = null
                entry.progress = -1
                entry.bytesDownloaded = 0
                entry.status = DownloadEntryStatus.QUEUED
            }
        }
    }

    override suspend fun submit(requests: List<DownloadRequest>): SubmitOutcome {
        // Read per call, never cached: preferences are not reactive in this app, so reading once at
        // construction would make the toggle require an app restart.
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
                        // Every request is already live in an earlier batch. A batch id with no
                        // entries can never gain any and the caller would observe it forever, so
                        // hand back the batch that actually owns the work.
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
            // Downloads must keep going with the app off screen, and a transcode cannot be
            // resumed if the process is frozen and the connection dropped.
            onWorkAccepted()
        }
        wake.trySend(Unit)
        return outcome
    }

    override suspend fun cancel(itemIds: Set<UUID>) {
        // Remove under the lock FIRST, so nextWorkLocked() cannot pick a victim and awaitTerminal
        // sees the identity mismatch and bails out immediately.
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
        // On the queue's own scope, not the caller's: the view model that asked for the cancel is
        // usually about to be cleared, and the cleanup must not die with it.
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
                // A succeeded entry's file is already on disk, and its sources row still carries
                // the download id, so cancelDownload() would find it and deleteItem() it. Cancel
                // means stop, not delete what has already finished.
                removed.filterNot { it.status == DownloadEntryStatus.SUCCEEDED }
            }
        // On the queue's own scope, not the caller's: the view model that asked for the cancel is
        // usually about to be cleared, and the cleanup must not die with it.
        scope.launch { removeVictims(victims) }
    }

    /**
     * Deliberately does NOT cancel a victim's prepare job. [Downloader.downloadItem] enqueues with
     * DownloadManager well before it writes the sources row carrying the download id, with
     * suspending network calls in between — cancelling in that window would strand a full size
     * file on disk that no database row, and therefore no part of the app, can ever reach.
     * [prepare] already notices its entry has been removed and cleans the enqueue up itself, so
     * an in flight entry is simply left to finish and tidy up after itself.
     */
    private suspend fun removeVictims(victims: List<Entry>) {
        for (victim in victims) {
            // QUEUED was never handed to downloadItem, so there is nothing of ours to cancel;
            // skipping it also keeps the fallback lookup from finding a sources row left by some
            // unrelated, already finished download and deleting it. PREPARING is still inside
            // downloadItem and is cleaned up by prepare()'s own orphan branch.
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
                // Safety net: an entry left non terminal would make nextWorkLocked() hand back the
                // same entry forever and stall every future download.
                mutex.withLock {
                    // QUEUED is deliberate: a retry puts the entry back in line, and forcing that
                    // to FAILED here would mean the retry never happened.
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

    /** Caller holds [mutex]. Flips a chosen entry to PREPARING so it cannot be picked twice. */
    private fun nextWorkLocked(): Work? {
        val busy =
            entries.values.firstOrNull {
                !it.status.isTerminal && it.status != DownloadEntryStatus.QUEUED
            }
        if (busy != null) {
            // Only reachable for an entry restored from a previous run: in the normal path the
            // dispatcher is already watching the busy entry and does not come back for more work
            // until it goes terminal. It is with DownloadManager already, so it must not be
            // enqueued a second time.
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

        // A download for this exact source may already be in flight, left behind by a queue that
        // died before it could be persisted. Adopting it is what stops a second copy of the same
        // file being fetched alongside the first.
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
            // Resolved outside the lock: a null row means DownloadManager has no record of the id,
            // and only the database can say whether DownloadReceiver already stripped the
            // ".download" suffix (success) or the job was simply lost.
            val renamed = if (info == null) isAlreadyDownloaded(entry) else false

            val terminal =
                mutex.withLock {
                    if (entries[entry.itemId] !== entry) {
                        true
                    } else {
                        val before = entry.durable
                        applyInfoLocked(entry, info, renamed)
                        publishLocked()
                        // Only when something worth keeping changed: applyInfoLocked runs on every
                        // poll, and rewriting an unchanged status once a second is pure churn.
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

    /** Caller holds [mutex]. */
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
                    // A transcode answers Accept-Ranges: none, so DownloadManager cannot pick up
                    // where it left off and reports failure instead. Starting over is the only
                    // way to finish, and beats leaving the item stranded.
                    entry.retries++
                    entry.downloadId = null
                    entry.progress = -1
                    // A restarted transcode begins from zero; keeping the old count would show
                    // progress running backwards.
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
     * Drops finished entries once the queue has been idle for a while, so a completed season does
     * not pin its items for the rest of the process. The grace period matters: clearing the moment
     * the last entry goes terminal would let StateFlow conflation swallow the terminal snapshot,
     * and collectors would never see the batch finish.
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

    /** Caller holds [mutex]. Drops totals for batches that no longer have any entries. */
    private fun dropOrphanedBatchTotalsLocked() {
        val liveBatches = entries.values.mapTo(mutableSetOf()) { it.batchId }
        batchTotals.keys.retainAll(liveBatches)
    }

    /** Caller holds [mutex]. Drops batches whose entries have all reached a terminal status. */
    private fun pruneTerminalBatchesLocked(): List<UUID> {
        val liveBatches =
            entries.values.filter { !it.status.isTerminal }.mapTo(mutableSetOf()) { it.batchId }
        val dropped = entries.values.filter { it.batchId !in liveBatches }.map { it.itemId }
        entries.values.removeAll { it.batchId !in liveBatches }
        batchTotals.keys.retainAll(liveBatches)
        return dropped
    }

    /**
     * Losing the mirror costs the ability to resume, not the download itself, so a failure here is
     * logged and swallowed rather than allowed to take the queue down.
     */
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

    /** Caller holds [mutex]. */
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
     * Whether a failure is the kind that starting over could fix. A dropped or truncated
     * connection is; running out of room, or a destination that has gone away, is not — retrying
     * those just burns the server's transcode budget to fail the same way.
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

/** Whether DownloadManager considers a download over, one way or the other. */
private val Int.isFinishedDownload: Boolean
    get() = this == DownloadManager.STATUS_SUCCESSFUL || this == DownloadManager.STATUS_FAILED

private val FindroidItem.queuedItemType: QueuedItemType
    get() = if (this is FindroidEpisode) QueuedItemType.EPISODE else QueuedItemType.MOVIE
