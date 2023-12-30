package dev.jdtech.jellyfin.viewmodels

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class EpisodeBottomSheetViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val database: ServerDatabaseDao,
    private val downloader: Downloader,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _downloadStatus = MutableStateFlow(Pair(0, 0))
    val downloadStatus = _downloadStatus.asStateFlow()

    private val eventsChannel = Channel<EpisodeBottomSheetEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    private val handler = Handler(Looper.getMainLooper())

    sealed class UiState {
        data class Normal(
            val episode: FindroidEpisode,
        ) : UiState()

        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidEpisode

    private var currentUiState: UiState = UiState.Loading

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = repository.getEpisode(episodeId)
                if (item.isDownloading()) {
                    pollDownloadProgress()
                }
                currentUiState = UiState.Normal(item)
                _uiState.emit(currentUiState)
            } catch (_: NullPointerException) {
                // Navigate back because item does not exist (probably because it's been deleted)
                eventsChannel.send(EpisodeBottomSheetEvent.NavigateBack)
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun togglePlayed() {
        suspend fun updateUiPlayedState(played: Boolean) {
            item = item.copy(played = played)
            when (currentUiState) {
                is UiState.Normal -> {
                    currentUiState = (currentUiState as UiState.Normal).copy(episode = item)
                    _uiState.emit(currentUiState)
                }

                else -> {}
            }
        }

        viewModelScope.launch {
            val originalPlayedState = item.played
            updateUiPlayedState(!item.played)

            when (item.played) {
                false -> {
                    try {
                        repository.markAsUnplayed(item.id)
                    } catch (_: Exception) {
                        updateUiPlayedState(originalPlayedState)
                    }
                }
                true -> {
                    try {
                        repository.markAsPlayed(item.id)
                    } catch (_: Exception) {
                        updateUiPlayedState(originalPlayedState)
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        suspend fun updateUiFavoriteState(isFavorite: Boolean) {
            item = item.copy(favorite = isFavorite)
            when (currentUiState) {
                is UiState.Normal -> {
                    currentUiState = (currentUiState as UiState.Normal).copy(episode = item)
                    _uiState.emit(currentUiState)
                }

                else -> {}
            }
        }

        viewModelScope.launch {
            val originalFavoriteState = item.favorite
            updateUiFavoriteState(!item.favorite)

            when (item.favorite) {
                false -> {
                    try {
                        repository.unmarkAsFavorite(item.id)
                    } catch (_: Exception) {
                        updateUiFavoriteState(originalFavoriteState)
                    }
                }
                true -> {
                    try {
                        repository.markAsFavorite(item.id)
                    } catch (_: Exception) {
                        updateUiFavoriteState(originalFavoriteState)
                    }
                }
            }
        }
    }

    fun download(sourceIndex: Int = 0, storageIndex: Int = 0) {
        viewModelScope.launch {
            val result = downloader.downloadItem(item, item.sources[sourceIndex].id, storageIndex)
            // Send one time signal to fragment that the download has been initiated
            _downloadStatus.emit(Pair(10, Random.nextInt()))

            if (result.second != null) {
                eventsChannel.send(EpisodeBottomSheetEvent.DownloadError(result.second!!))
            }

            loadEpisode(item.id)
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            downloader.cancelDownload(item, item.sources.first { it.type == FindroidSourceType.LOCAL })
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
                    val source = item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
                    val (downloadStatus, progress) = downloader.getProgress(source?.downloadId)
                    _downloadStatus.emit(Pair(downloadStatus, progress))
                    if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                        if (source == null) return@launch
                        val path = source.path.replace(".download", "")
                        File(source.path).renameTo(File(path))
                        database.setSourcePath(source.id, path)
                        loadEpisode(item.id)
                    }
                    if (downloadStatus == DownloadManager.STATUS_FAILED) {
                        if (source == null) return@launch
                        downloader.deleteItem(item, source)
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

sealed interface EpisodeBottomSheetEvent {
    data object NavigateBack : EpisodeBottomSheetEvent
    data class DownloadError(val uiText: UiText) : EpisodeBottomSheetEvent
}
