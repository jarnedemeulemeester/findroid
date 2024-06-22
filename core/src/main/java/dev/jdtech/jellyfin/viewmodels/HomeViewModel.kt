package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.toView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject internal constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val homeItems: List<HomeItem>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    private val uuidContinueWatching = UUID(4937169328197226115, -4704919157662094443) // 44845958-8326-4e83-beb4-c4f42e9eeb95
    private val uuidNextUp = UUID(1783371395749072194, -6164625418200444295) // 18bfced5-f237-4d42-aa72-d9d7fed19279

    private val uiTextContinueWatching = UiText.StringResource(R.string.continue_watching)
    private val uiTextNextUp = UiText.StringResource(R.string.next_up)

    init {
        viewModelScope.launch {
            try {
                repository.postCapabilities()
            } catch (_: Exception) { }
        }
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.emit(UiState.Loading)
            try {
                val items = mutableListOf<HomeItem>()

                if (appPreferences.offlineMode) items.add(HomeItem.OfflineCard)

                val updated = items + loadDynamicItems() + loadViews()

                _uiState.emit(UiState.Normal(updated))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    private suspend fun loadDynamicItems(): List<HomeItem.Section> {
        val resumeItems = repository.getResumeItems()
        val nextUpItems = repository.getNextUp()

        val items = mutableListOf<HomeSection>()
        if (resumeItems.isNotEmpty()) {
            items.add(
                HomeSection(
                    uuidContinueWatching,
                    uiTextContinueWatching,
                    resumeItems,
                ),
            )
        }

        if (nextUpItems.isNotEmpty()) {
            items.add(
                HomeSection(
                    uuidNextUp,
                    uiTextNextUp,
                    nextUpItems,
                ),
            )
        }

        return items.map { HomeItem.Section(it) }
    }

    private suspend fun loadViews() = repository
        .getUserViews()
        .filter { view -> CollectionType.fromString(view.collectionType?.serialName) in CollectionType.supported }
        .map { view -> view to repository.getLatestMedia(view.id) }
        .filter { (_, latest) -> latest.isNotEmpty() }
        .map { (view, latest) -> view.toView().apply { items = latest } }
        .map { HomeItem.ViewItem(it) }
}
