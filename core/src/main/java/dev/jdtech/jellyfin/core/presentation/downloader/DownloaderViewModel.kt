package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloaderViewModel
@Inject
constructor(
    private val downloader: Downloader,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloaderState())
    val state = _state.asStateFlow()

    var downloadId: Long? = null

    private val handler = Handler(Looper.getMainLooper())

    fun update(
        item: FindroidItem,
    ) {
        viewModelScope.launch {
            if (item.isDownloading()) {
                val source = item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: return@launch
                this@DownloaderViewModel.downloadId = source.downloadId
                pollDownloadProgress(source.downloadId)
            }
        }
    }

    private fun download(
        item: FindroidItem,
        storageIndex: Int = 0,
    ) {
        viewModelScope.launch {
            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            val (downloadId, uiText) = downloader.downloadItem(
                item = item,
                sourceId = item.sources.first().id,
                storageIndex = storageIndex,
            )
            if (downloadId != -1L) {
                this@DownloaderViewModel.downloadId = downloadId
                pollDownloadProgress(downloadId)
            } else {
                _state.emit(DownloaderState(status = DownloadManager.STATUS_FAILED))
            }
        }
    }

    private fun cancelDownload(
        item: FindroidItem,
    ) {
        viewModelScope.launch {
            downloadId?.let {
                downloader.cancelDownload(
                    item = item,
                    downloadId = it,
                )
            }
        }
    }

    private fun deleteDownload(
        item: FindroidItem,
    ) {
        viewModelScope.launch {
            downloader.deleteItem(
                item = item,
                source = item.sources.first(),
            )
        }
    }

    private fun pollDownloadProgress(
        downloadId: Long?,
    ) {
        handler.removeCallbacksAndMessages(null)
        val downloadProgressRunnable = object : Runnable {
            override fun run() {
                viewModelScope.launch {
                    val (status, progress) = downloader.getProgress(downloadId)
                    _state.emit(DownloaderState(status = status, progress = progress.coerceAtLeast(0) / 100f))
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
            is DownloaderAction.Download -> download(action.item)
            is DownloaderAction.DeleteDownload -> deleteDownload(action.item)
            is DownloaderAction.CancelDownload -> cancelDownload(action.item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
