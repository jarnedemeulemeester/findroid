package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.adapters.HomeItem.Section
import dev.jdtech.jellyfin.adapters.HomeItem.ViewItem
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.syncPlaybackProgress
import dev.jdtech.jellyfin.utils.toView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject internal constructor(
    private val application: Application,
    private val repository: JellyfinRepository,
    private val downloadDatabase: DownloadDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val homeItems: List<HomeItem>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        viewModelScope.launch {
            try {
                repository.postCapabilities()
            } catch (_: Exception) {
            }
        }
    }

    fun loadData(includeLibraries: Boolean = false) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val items = mutableListOf<HomeItem>()

                if (includeLibraries) {
                    items.add(loadLibraries())
                }

                val updated = items + loadDynamicItems() + loadViews()

                withContext(Dispatchers.Default) {
                    syncPlaybackProgress(downloadDatabase, repository)
                }
                _uiState.emit(UiState.Normal(updated))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    private suspend fun loadLibraries(): HomeItem {
        val items = repository.getItems()
        val collections =
            items.filter { collection -> CollectionType.unsupportedCollections.none { it.type == collection.collectionType } }
        return HomeItem.Libraries(
            HomeSection(
                UUID.fromString("38f5ca96-9e4b-4c0e-a8e4-02225ed07e02"),
                application.resources.getString(R.string.libraries),
                collections
            )
        )
    }

    private suspend fun loadDynamicItems(): List<Section> {
        val resumeItems = repository.getResumeItems()
        val nextUpItems = repository.getNextUp()

        val items = mutableListOf<HomeSection>()
        if (resumeItems.isNotEmpty()) {
            items.add(
                HomeSection(
                    UUID.fromString("44845958-8326-4e83-beb4-c4f42e9eeb95"),
                    application.resources.getString(R.string.continue_watching),
                    resumeItems
                )
            )
        }

        if (nextUpItems.isNotEmpty()) {
            items.add(
                HomeSection(
                    UUID.fromString("18bfced5-f237-4d42-aa72-d9d7fed19279"),
                    application.resources.getString(R.string.next_up),
                    nextUpItems
                )
            )
        }

        return items.map { Section(it) }
    }

    private suspend fun loadViews() = repository
        .getUserViews()
        .filter { view -> CollectionType.unsupportedCollections.none { it.type == view.collectionType } }
        .map { view -> view to repository.getLatestMedia(view.id) }
        .filter { (_, latest) -> latest.isNotEmpty() }
        .map { (view, latest) -> view.toView().apply { items = latest } }
        .map { ViewItem(it) }
}


