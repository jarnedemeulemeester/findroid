package dev.jdtech.jellyfin.viewmodels

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.exception.ApiClientException
import timber.log.Timber

@HiltViewModel
class EpisodeBottomSheetViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val downloader: Downloader,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _downloadStatus = MutableStateFlow(Pair(0, 0))
    val downloadStatus = _downloadStatus.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    sealed class UiState {
        data class Normal(
            val episode: FindroidEpisode,
        ) : UiState()

        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidEpisode

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = jellyfinRepository.getEpisode(episodeId)
                if (item.isDownloading()) {
                    pollDownloadProgress()
                }
                _uiState.emit(
                    UiState.Normal(
                        item,
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun togglePlayed() {
        viewModelScope.launch {
            try {
                if (item.played) {
                    jellyfinRepository.markAsUnplayed(item.id)
                } else {
                    jellyfinRepository.markAsPlayed(item.id)
                }
                loadEpisode(item.id)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                if (item.favorite) {
                    jellyfinRepository.unmarkAsFavorite(item.id)
                } else {
                    jellyfinRepository.markAsFavorite(item.id)
                }
                loadEpisode(item.id)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    fun download(sourceIndex: Int = 0) {
        viewModelScope.launch {
            downloader.downloadItem(item, item.sources[sourceIndex].id)
            loadEpisode(item.id)
        }
    }

    fun deleteEpisode() {
        viewModelScope.launch {
            downloader.deleteItem(item, item.sources.first { it.type == FindroidSourceType.LOCAL })
            loadEpisode(item.id)
        }
    }

    private fun pollDownloadProgress() {
        handler.removeCallbacksAndMessages(null)
        val downloadProgressRunnable = object : Runnable {
            override fun run() {
                viewModelScope.launch {
                    val (downloadStatus, progress) = downloader.getProgress(item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }?.downloadId)
                    _downloadStatus.emit(Pair(downloadStatus, progress))
                    if (downloadStatus != DownloadManager.STATUS_RUNNING && downloadStatus != DownloadManager.STATUS_PENDING) {
                        loadEpisode(item.id)
                    }
                }
                if (item.isDownloading()) {
                    handler.postDelayed(this, 2000L)
                }
            }
        }
        handler.post(downloadProgressRunnable)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
