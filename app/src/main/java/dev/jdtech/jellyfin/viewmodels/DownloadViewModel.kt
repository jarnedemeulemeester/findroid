package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dev.jdtech.jellyfin.models.DownloadSection
import dev.jdtech.jellyfin.utils.loadDownloadedEpisodes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class DownloadViewModel : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val downloadSections: List<DownloadSection>) : UiState()
        object Loading : UiState()
        data class Error(val message: String?) : UiState()
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
                val items = loadDownloadedEpisodes()
                if (items.isEmpty()) {
                    uiState.emit(UiState.Normal(emptyList()))
                    return@launch
                }
                val downloadSections = mutableListOf<DownloadSection>()
                withContext(Dispatchers.Default) {
                    DownloadSection(
                        UUID.randomUUID(),
                        "Episodes",
                        items.filter { it.metadata?.type == "Episode" }).let {
                        if (it.items.isNotEmpty()) downloadSections.add(
                            it
                        )
                    }
                    DownloadSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.metadata?.type == "Movie" }).let {
                        if (it.items.isNotEmpty()) downloadSections.add(
                            it
                        )
                    }
                }
                uiState.emit(UiState.Normal(downloadSections))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e.message))
            }
        }
    }
}