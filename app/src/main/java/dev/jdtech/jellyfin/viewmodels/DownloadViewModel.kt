package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.ContentType
import dev.jdtech.jellyfin.models.DownloadSection
import dev.jdtech.jellyfin.utils.loadDownloadedEpisodes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel
@Inject
constructor(
    private val downloadDatabase: DownloadDatabaseDao,
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val downloadSections: List<DownloadSection>) : UiState()
        object Loading : UiState()
        data class Error(val title: String?, val message: String?) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val items = loadDownloadedEpisodes(downloadDatabase)
                val downloadSections = mutableListOf<DownloadSection>()
                withContext(Dispatchers.Default) {
                    DownloadSection(
                        UUID.randomUUID(),
                        "Episodes",
                        items.filter { it.item?.type == ContentType.EPISODE }).let {
                        if (it.items.isNotEmpty()) downloadSections.add(
                            it
                        )
                    }
                    DownloadSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.item?.type == ContentType.MOVIE }).let {
                        if (it.items.isNotEmpty()) downloadSections.add(
                            it
                        )
                    }
                }
                uiState.emit(UiState.Normal(downloadSections))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e.message, e.stackTraceToString()))
            }
        }
    }
}