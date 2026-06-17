package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DownloaderViewModel @Inject constructor(private val downloader: Downloader) : ViewModel() {
    private val _state = MutableStateFlow(DownloaderState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DownloaderEvent>()
    val events = eventsChannel.receiveAsFlow()

    var downloadId: Long? = null
    var downloadItem: FindroidItem? = null

    private val handler = Handler(Looper.getMainLooper())
    private var seasonDownloader: Job? = null

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

    // TODO JONAS when is this called? Not sure it is necessary
//    fun update(episodes: List<FindroidEpisode>) {
//        viewModelScope.launch {
//            episodes.firstOrNull {
//                it.isDownloading()
//            }?.let { item ->
//                val source =
//                    item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
//                        ?: return@launch
//                this@DownloaderViewModel.downloadId = source.downloadId
//                pollDownloadProgress(source.downloadId)
//            }
//        }
//    }

    private fun download(item: FindroidItem, storageIndex: Int = 0) {
        viewModelScope.launch {
            downloadInner(item, storageIndex)
        }
    }

    // TODO JONAS move back inside function
    private suspend fun downloadInner(item: FindroidItem, storageIndex: Int = 0) {
        _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
        val (downloadId, uiText) =
            downloader.downloadItem(
                item = item,
                sourceId = item.sources.first().id,
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

    // TODO JONAS not happy about progress reporting
    private fun downloadSeason(episodes: List<FindroidEpisode>, storageIndex: Int) {
        // Safeguard against launching two download jobs at once
        if (seasonDownloader?.isActive == true) {
            return
        }

        // TODO JONAS dispatcher?
        seasonDownloader = viewModelScope.launch(Dispatchers.Default) {
            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            episodes.forEachIndexed { index, episode ->
                _state.emit(
                    DownloaderState(
                        status = DownloadManager.STATUS_RUNNING,
                        progress = index.toFloat() / episodes.size,
                    )
                )

                // If already downloaded skip
                if (episode.sources.any { src -> src.type == FindroidSourceType.LOCAL }) {
                    Log.d("JONAS", "Already downloaded: ${episode.id}")
                    return@forEachIndexed
                }

                val (downloadId, uiText) =
                    downloader.downloadItem(
                        item = episode,
                        sourceId = episode.sources.first().id,
                        storageIndex = storageIndex,
                    )

                if (downloadId == -1L) {
                    _state.emit(
                        DownloaderState(status = DownloadManager.STATUS_FAILED, errorText = uiText)
                    )
                    Log.d("JONAS", "Failed before download: ${_state.value}")
                    // Trigger fetch
                    eventsChannel.trySend(DownloaderEvent.Successful)
                    return@launch
                }

                // Keep track so we can cancel the download
                this@DownloaderViewModel.downloadItem = episode
                this@DownloaderViewModel.downloadId = downloadId
                var status = DownloadManager.STATUS_RUNNING
                while (status == DownloadManager.STATUS_RUNNING) {
                    delay(1000L)
                    status = downloader.getProgress(downloadId).first
                }

                if (status == DownloadManager.STATUS_FAILED) {
                    _state.emit(
                        DownloaderState(status = DownloadManager.STATUS_FAILED, errorText = uiText)
                    )
                    Log.d("JONAS", "Failed during download: ${_state.value}")
                    // Trigger fetching of data
                    eventsChannel.trySend(DownloaderEvent.Successful)
                    return@launch
                }

//                Log.d("JONAS", "Download ${episode.seriesName} ${episode.id}")
//                downloadInner(episode, storageIndex)
//                if (_state.value.status == DownloadManager.STATUS_FAILED) {
//                    Log.d("JONAS", "Early abort, something failed: ${_state.value}")
//                    break
//                }
            }

            _state.emit(
                DownloaderState(status = DownloadManager.STATUS_SUCCESSFUL)
            )
            eventsChannel.trySend(DownloaderEvent.Successful)
        }
    }

    private fun cancelDownload(item: FindroidItem) {
        viewModelScope.launch {
            // Stop progress polling
            handler.removeCallbacksAndMessages(null)

            // Cancel the download
            downloadId?.let { downloader.cancelDownload(item = item, downloadId = it) }

            // Emit empty DownloadState
            _state.emit(DownloaderState())
        }
    }

    private fun cancelDownloadSeason() {
        viewModelScope.launch(Dispatchers.Default) {
            // Stop the download job
            seasonDownloader?.cancel("User pressed cancel button")

            // Cancel the download
            downloadId?.let { downloadId ->
                downloadItem?.let { downloadItem ->
                    downloader.cancelDownload(item = downloadItem, downloadId = downloadId)
                }
            }

            // Emit empty DownloadState
            _state.emit(DownloaderState())
            // Send event to trigger reload
            eventsChannel.send(DownloaderEvent.Deleted)
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

    private fun deleteDownloadedSeason(episodes: List<FindroidEpisode>) {
        // Only if a source has type local can it be deleted
        episodes
            .filter { ep -> ep.sources.any { src -> src.type == FindroidSourceType.LOCAL } }
            .forEach(::deleteDownload)
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
                                progress = progress.coerceAtLeast(0) / 100f,
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
            is DownloaderAction.DownloadSeason -> downloadSeason(action.episodes, action.storageIndex)
            is DownloaderAction.DeleteDownload -> deleteDownload(action.item)
            is DownloaderAction.DeleteDownloadedSeason -> deleteDownloadedSeason(action.episodes)
            is DownloaderAction.CancelDownload -> cancelDownload(action.item)
            DownloaderAction.CancelDownloadSeason -> cancelDownloadSeason()
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        seasonDownloader?.cancel("onCleared")
    }
}
