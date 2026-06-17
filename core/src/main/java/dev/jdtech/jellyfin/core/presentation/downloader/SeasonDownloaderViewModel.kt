package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.Downloader
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SeasonDownloaderViewModel @Inject constructor(private val downloader: Downloader) :
    ViewModel() {
    private val _state = MutableStateFlow(DownloaderState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DownloaderEvent>()
    val events = eventsChannel.receiveAsFlow()

    // Maps downloadId -> episode so cancel can look up both pieces needed by Downloader.cancelDownload
    private val activeDownloads = mutableMapOf<Long, FindroidEpisode>()

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Called whenever the episode list changes (e.g. screen resume). Resumes progress polling if
     * any episode is still being downloaded.
     */
    fun update(episodes: List<FindroidEpisode>) {
        Log.d("JONAS", "Updating season downloader view model episodes: ${episodes.size}")
        val downloading = episodes.filter { it.isDownloading() }
        if (downloading.isNotEmpty()) {
            activeDownloads.clear()
            downloading.forEach { episode ->
                episode.sources
                    .firstOrNull { it.type == FindroidSourceType.LOCAL }
                    ?.downloadId
                    ?.let { id -> activeDownloads[id] = episode }
            }
            pollAggregateProgress()
        }
    }

    private fun download(episodes: List<FindroidEpisode>, storageIndex: Int) {
        viewModelScope.launch {
            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            activeDownloads.clear()

            val toDownload = episodes.filter { !it.isDownloaded() && !it.isDownloading() }
            if (toDownload.isEmpty()) {
                eventsChannel.send(DownloaderEvent.Successful)
                return@launch
            }

            for (episode in toDownload) {
                if (episode.sources.isEmpty()) {
                    // TODO JONAS yes it is emtpy
                    Log.d("JONAS", "VM episode sources is empty")
                }
                val sourceId = episode.sources.firstOrNull()?.id ?: continue
                val (downloadId, error) =
                    downloader.downloadItem(
                        item = episode,
                        sourceId = sourceId,
                        storageIndex = storageIndex,
                    )
                if (downloadId != -1L) {
                    activeDownloads[downloadId] = episode
                } else {
                    _state.emit(
                        DownloaderState(status = DownloadManager.STATUS_FAILED, errorText = error)
                    )
                    return@launch
                }
            }

            pollAggregateProgress()
        }
    }

    private fun cancelDownload() {
        viewModelScope.launch {
            handler.removeCallbacksAndMessages(null)
            activeDownloads.forEach { (downloadId, episode) ->
                downloader.cancelDownload(item = episode, downloadId = downloadId)
            }
            activeDownloads.clear()
            _state.emit(DownloaderState())
        }
    }

    private fun deleteDownload(episodes: List<FindroidEpisode>) {
        viewModelScope.launch {
            episodes
                .filter { it.isDownloaded() }
                .forEach { episode ->
                    val source =
                        episode.sources.first { it.type == FindroidSourceType.LOCAL }
                    downloader.deleteItem(item = episode, source = source)
                }
            activeDownloads.clear()
            eventsChannel.send(DownloaderEvent.Deleted)
        }
    }

    private fun pollAggregateProgress() {
        handler.removeCallbacksAndMessages(null)
        val runnable =
            object : Runnable {
                override fun run() {
                    viewModelScope.launch {
                        if (activeDownloads.isEmpty()) return@launch

                        var totalProgress = 0
                        var allSuccessful = true
                        var anyFailed = false

                        for (id in activeDownloads.keys) {
                            val (status, progress) = downloader.getProgress(id)
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {}
                                DownloadManager.STATUS_FAILED -> {
                                    anyFailed = true
                                    allSuccessful = false
                                }
                                else -> allSuccessful = false
                            }
                            totalProgress += progress.coerceAtLeast(0)
                        }

                        val avgProgress = totalProgress / activeDownloads.size
                        val aggregateStatus =
                            when {
                                anyFailed -> DownloadManager.STATUS_FAILED
                                allSuccessful -> DownloadManager.STATUS_SUCCESSFUL
                                else -> DownloadManager.STATUS_RUNNING
                            }

                        _state.emit(
                            DownloaderState(
                                status = aggregateStatus,
                                progress = avgProgress / 100f,
                            )
                        )

                        if (aggregateStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            eventsChannel.trySend(DownloaderEvent.Successful)
                        }
                    }

                    if (_state.value.isDownloading) {
                        handler.postDelayed(this, 1000L)
                    }
                }
            }
        handler.post(runnable)
    }

    fun onAction(action: SeasonDownloaderAction) {
        when (action) {
            is SeasonDownloaderAction.Download -> download(action.episodes, action.storageIndex)
            is SeasonDownloaderAction.DeleteDownload -> deleteDownload(action.episodes)
            is SeasonDownloaderAction.CancelDownload -> cancelDownload()
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
