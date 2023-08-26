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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _downloadError = MutableSharedFlow<UiText>()
    val downloadError = _downloadError.asSharedFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack = _navigateBack.asSharedFlow()

    private val handler = Handler(Looper.getMainLooper())

    sealed class UiState {
        data class Normal(
            val episode: FindroidEpisode,
        ) : UiState()

        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidEpisode
    private var played: Boolean = false
    private var favorite: Boolean = false

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = repository.getEpisode(episodeId)
                played = item.played
                favorite = item.favorite
                if (item.isDownloading()) {
                    pollDownloadProgress()
                }
                _uiState.emit(
                    UiState.Normal(
                        item,
                    ),
                )
            } catch (_: NullPointerException) {
                // Navigate back because item does not exist (probably because it's been deleted)
                _navigateBack.emit(true)
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun togglePlayed(): Boolean {
        when (played) {
            false -> {
                played = true
                viewModelScope.launch {
                    try {
                        repository.markAsPlayed(item.id)
                    } catch (_: Exception) {}
                }
            }
            true -> {
                played = false
                viewModelScope.launch {
                    try {
                        repository.markAsUnplayed(item.id)
                    } catch (_: Exception) {}
                }
            }
        }
        return played
    }

    fun toggleFavorite(): Boolean {
        when (favorite) {
            false -> {
                favorite = true
                viewModelScope.launch {
                    try {
                        repository.markAsFavorite(item.id)
                    } catch (_: Exception) {}
                }
            }
            true -> {
                favorite = false
                viewModelScope.launch {
                    try {
                        repository.unmarkAsFavorite(item.id)
                    } catch (_: Exception) {}
                }
            }
        }
        return favorite
    }

    fun download(sourceIndex: Int = 0, storageIndex: Int = 0) {
        viewModelScope.launch {
            val result = downloader.downloadItem(item, item.sources[sourceIndex].id, storageIndex)
            // Send one time signal to fragment that the download has been initiated
            _downloadStatus.emit(Pair(10, Random.nextInt()))

            if (result.second != null) {
                _downloadError.emit(result.second!!)
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
