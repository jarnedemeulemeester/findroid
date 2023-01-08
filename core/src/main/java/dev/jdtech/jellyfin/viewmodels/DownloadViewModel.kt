package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.DownloadSection
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkDownloadStatus
import dev.jdtech.jellyfin.utils.loadDownloadedEpisodes
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind

@HiltViewModel
class DownloadViewModel
@Inject
constructor(
    private val application: Application,
    private val downloadDatabase: DownloadDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val downloadSections: List<DownloadSection>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                checkDownloadStatus(downloadDatabase, application)
                val items = loadDownloadedEpisodes(downloadDatabase)

                val showsMap = mutableMapOf<UUID, MutableList<PlayerItem>>()
                items.filter { it.item?.type == BaseItemKind.EPISODE }.forEach {
                    showsMap.computeIfAbsent(it.item!!.seriesId!!) { mutableListOf() } += it
                }
                val shows = showsMap.map {
                    dev.jdtech.jellyfin.models.DownloadSeriesMetadata(
                        it.key,
                        it.value[0].item!!.seriesName,
                        it.value
                    )
                }

                val downloadSections = mutableListOf<DownloadSection>()
                withContext(Dispatchers.Default) {
                    DownloadSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.item?.type == BaseItemKind.MOVIE }
                    ).let {
                        if (it.items!!.isNotEmpty()) downloadSections.add(
                            it
                        )
                    }
                    DownloadSection(
                        UUID.randomUUID(),
                        "Shows",
                        null,
                        shows
                    ).let {
                        if (it.series!!.isNotEmpty()) downloadSections.add(
                            it
                        )
                    }
                }
                _uiState.emit(UiState.Normal(downloadSections))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }
}
