package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.DownloadEntry
import dev.jdtech.jellyfin.utils.DownloadEntryStatus
import dev.jdtech.jellyfin.utils.DownloadQueue
import dev.jdtech.jellyfin.utils.DownloadRequest
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.utils.SubmitOutcome
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DownloaderViewModel
@Inject
constructor(private val downloader: Downloader, private val downloadQueue: DownloadQueue) :
    ViewModel() {
    private val _state = MutableStateFlow(DownloaderState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DownloaderEvent>()
    val events = eventsChannel.receiveAsFlow()

    /**
     * The queue as a whole, for lists that want to show which of their items are waiting. A screen
     * showing one item wants [state]; a screen showing many wants this.
     */
    val queue = downloadQueue.state

    var downloadId: Long? = null

    private val handler = Handler(Looper.getMainLooper())

    /** Set while this view model is following a queued item instead of polling one itself. */
    private var queueJob: Job? = null

    fun update(item: FindroidItem) {
        viewModelScope.launch {
            if (item.isDownloading()) {
                val source =
                    item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
                        ?: return@launch
                this@DownloaderViewModel.downloadId = source.downloadId
                pollDownloadProgress(source.downloadId)
            }
        }
    }

    private fun download(item: FindroidItem, storageIndex: Int = 0) {
        viewModelScope.launch {
            val sourceId = item.sources.firstOrNull()?.id ?: return@launch

            // Reads the preference itself and hands back Bypassed when sequential downloads are
            // off, so with the setting off this path is exactly what it was before.
            val outcome =
                downloadQueue.submit(
                    listOf(
                        DownloadRequest(
                            item = item,
                            sourceId = sourceId,
                            storageIndex = storageIndex,
                        )
                    )
                )
            if (outcome is SubmitOutcome.Queued) {
                follow(item.id)
                return@launch
            }

            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            val (downloadId, uiText) =
                downloader.downloadItem(
                    item = item,
                    sourceId = sourceId,
                    storageIndex = storageIndex,
                )
            if (downloadId != -1L) {
                this@DownloaderViewModel.downloadId = downloadId
                pollDownloadProgress(downloadId)
            } else {
                _state.emit(
                    DownloaderState(status = DownloadManager.STATUS_FAILED, errorText = uiText)
                )
            }
        }
    }

    /**
     * Reports what the queue says about [itemId], rather than polling DownloadManager. The queue is
     * the only thing that knows an item is waiting its turn, which DownloadManager cannot express:
     * it has not been handed the download yet.
     */
    private fun follow(itemId: UUID) {
        handler.removeCallbacksAndMessages(null)
        queueJob?.cancel()
        queueJob =
            viewModelScope.launch {
                downloadQueue.state.collect { snapshot ->
                    val entry = snapshot.entries.firstOrNull { it.itemId == itemId }
                    if (entry == null) {
                        // Cancelled, or pruned long after finishing. Either way there is nothing
                        // left to report.
                        _state.emit(DownloaderState())
                        return@collect
                    }
                    _state.emit(entry.toDownloaderState(snapshot.entries))
                    if (entry.status == DownloadEntryStatus.SUCCEEDED) {
                        eventsChannel.trySend(DownloaderEvent.Successful)
                    }
                }
            }
    }

    /**
     * Submits a whole season as one batch, in episode order, so the episodes a viewer will watch
     * first are the ones that finish first.
     */
    private fun downloadMany(items: List<FindroidItem>, storageIndex: Int = 0) {
        viewModelScope.launch {
            val pending =
                items
                    .filterNot { item -> item.sources.any { it.type == FindroidSourceType.LOCAL } }
                    .sortedWith(
                        compareBy(
                            { (it as? FindroidEpisode)?.parentIndexNumber ?: 0 },
                            { (it as? FindroidEpisode)?.indexNumber ?: 0 },
                        )
                    )
            val requests =
                pending.mapNotNull { item ->
                    item.sources.firstOrNull()?.let { source ->
                        DownloadRequest(
                            item = item,
                            sourceId = source.id,
                            storageIndex = storageIndex,
                        )
                    }
                }
            if (requests.isEmpty()) return@launch

            val outcome = downloadQueue.submit(requests)
            if (outcome is SubmitOutcome.Queued) {
                followBatch(outcome.batchId)
                return@launch
            }

            // Sequential downloads are off, so this behaves like asking for each of them
            // separately: they all go to DownloadManager and share the connection.
            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            for (request in requests) {
                downloader.downloadItem(
                    item = request.item,
                    sourceId = request.sourceId,
                    storageIndex = request.storageIndex,
                )
            }
            eventsChannel.trySend(DownloaderEvent.Successful)
            _state.emit(DownloaderState())
        }
    }

    /** Reports the item the batch is working on now, with how far through the batch it is. */
    private fun followBatch(batchId: UUID) {
        handler.removeCallbacksAndMessages(null)
        queueJob?.cancel()
        queueJob =
            viewModelScope.launch {
                downloadQueue.state.collect { snapshot ->
                    val batch = snapshot.entries.filter { it.batchId == batchId }
                    if (batch.isEmpty()) {
                        _state.emit(DownloaderState())
                        return@collect
                    }
                    val active = batch.firstOrNull { !it.status.isTerminal }
                    if (active == null) {
                        eventsChannel.trySend(DownloaderEvent.Successful)
                        _state.emit(DownloaderState())
                        return@collect
                    }
                    _state.emit(active.toDownloaderState(batch))
                }
            }
    }

    private fun deleteDownloadMany(items: List<FindroidItem>) {
        viewModelScope.launch {
            items
                .filter { item -> item.sources.any { it.type == FindroidSourceType.LOCAL } }
                .forEach { item ->
                    downloader.deleteItem(
                        item = item,
                        source = item.sources.first { it.type == FindroidSourceType.LOCAL },
                    )
                }
            eventsChannel.send(DownloaderEvent.Deleted)
        }
    }

    /** Drops the whole batch, including the episodes still waiting behind the one in flight. */
    private fun cancelDownloadMany() {
        viewModelScope.launch {
            handler.removeCallbacksAndMessages(null)
            queueJob?.cancel()
            val batchId = downloadQueue.state.value.entries.firstOrNull()?.batchId
            if (batchId != null) {
                downloadQueue.cancelBatch(batchId)
            }
            _state.emit(DownloaderState())
        }
    }

    private fun cancelDownload(item: FindroidItem) {
        viewModelScope.launch {
            // Stop progress polling
            handler.removeCallbacksAndMessages(null)
            queueJob?.cancel()

            // The queue owns the download when it accepted the item, and cancelling through it
            // also drops whatever is still waiting behind it.
            if (downloadQueue.state.value.entries.any { it.itemId == item.id }) {
                downloadQueue.cancel(setOf(item.id))
            } else {
                downloadId?.let { downloader.cancelDownload(item = item, downloadId = it) }
            }

            // Emit empty DownloadState
            _state.emit(DownloaderState())
        }
    }

    private fun deleteDownload(item: FindroidItem) {
        viewModelScope.launch {
            downloader.deleteItem(
                item = item,
                source = item.sources.first { it.type == FindroidSourceType.LOCAL },
            )
            eventsChannel.send(DownloaderEvent.Deleted)
        }
    }

    private fun pollDownloadProgress(downloadId: Long?) {
        handler.removeCallbacksAndMessages(null)
        val downloadProgressRunnable =
            object : Runnable {
                override fun run() {
                    viewModelScope.launch {
                        val (status, progress) = downloader.getProgress(downloadId)
                        _state.emit(
                            DownloaderState(
                                status = status,
                                // takeIf, not coerceAtLeast: -1 means the total size is unknown,
                                // and clamping it to 0 would draw a download that is moving as one
                                // stuck at 0%.
                                progress = progress.takeIf { it >= 0 }?.div(100f),
                            )
                        )
                    }

                    if (_state.value.status == DownloadManager.STATUS_SUCCESSFUL) {
                        eventsChannel.trySend(DownloaderEvent.Successful)
                    }

                    if (_state.value.isDownloading) {
                        handler.postDelayed(this, 1000L)
                    }
                }
            }
        handler.post(downloadProgressRunnable)
    }

    fun onAction(action: DownloaderAction) {
        when (action) {
            is DownloaderAction.Download -> download(action.item, action.storageIndex)
            is DownloaderAction.DownloadMany -> downloadMany(action.items, action.storageIndex)
            is DownloaderAction.DeleteDownload -> deleteDownload(action.item)
            is DownloaderAction.DeleteDownloadMany -> deleteDownloadMany(action.items)
            is DownloaderAction.CancelDownload -> cancelDownload(action.item)
            is DownloaderAction.CancelDownloadMany -> cancelDownloadMany()
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        queueJob?.cancel()
    }
}

/**
 * The state of one queued item, with a count of everything still outstanding beside it.
 *
 * The count is deliberately of the whole queue rather than of some batch: an item only waits
 * because other items are ahead of it, and "3 left" is what explains why nothing appears to be
 * happening to this one yet.
 */
internal fun DownloadEntry.toDownloaderState(all: List<DownloadEntry>): DownloaderState {
    val status =
        when (status) {
            DownloadEntryStatus.RUNNING -> DownloadManager.STATUS_RUNNING
            DownloadEntryStatus.PAUSED -> DownloadManager.STATUS_PAUSED
            DownloadEntryStatus.SUCCEEDED -> DownloadManager.STATUS_SUCCESSFUL
            DownloadEntryStatus.FAILED -> DownloadManager.STATUS_FAILED
            // QUEUED or PREPARING: accepted, but no bytes are moving for it yet.
            else -> DownloadManager.STATUS_PENDING
        }
    val outstanding = all.count { !it.status.isTerminal }
    val done = all.count { it.status == DownloadEntryStatus.SUCCEEDED }
    val counted = all.size > 1
    return DownloaderState(
        status = status,
        // -1 means the total size is unknown, which is not the same as no progress.
        progress = progress.takeIf { it >= 0 }?.div(100f)?.coerceIn(0f, 1f),
        bytesDownloaded = bytesDownloaded,
        errorText = errorText,
        itemsCompleted = (done + 1).coerceAtMost(all.size).takeIf { counted && outstanding > 0 },
        itemsTotal = all.size.takeIf { counted && outstanding > 0 },
    )
}
