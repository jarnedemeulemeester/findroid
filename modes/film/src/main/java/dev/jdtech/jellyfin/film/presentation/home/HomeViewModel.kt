package dev.jdtech.jellyfin.film.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.toView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import dev.jdtech.jellyfin.film.R as FilmR

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    val repository: JellyfinRepository,
    val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val uuidContinueWatching = UUID(4937169328197226115, -4704919157662094443) // 44845958-8326-4e83-beb4-c4f42e9eeb95
    private val uuidNextUp = UUID(1783371395749072194, -6164625418200444295) // 18bfced5-f237-4d42-aa72-d9d7fed19279

    private val uiTextContinueWatching = UiText.StringResource(FilmR.string.continue_watching)
    private val uiTextNextUp = UiText.StringResource(FilmR.string.next_up)

    fun loadData() {
        viewModelScope.launch(Dispatchers.Default) {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                if (appPreferences.getValue(appPreferences.offlineMode)) _state.emit(_state.value.copy(isOffline = true))

                // Load sequentially instead of in parallel because if the views load faster then the dynamic items, the dynamic items will appear above it but the scroll position will remain the same creating a weird look.
                loadDynamicItems()
                loadViews()
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
            _state.emit(_state.value.copy(isLoading = false))
        }
    }

    private suspend fun loadDynamicItems() {
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

        _state.emit(
            _state.value.copy(sections = items.map { HomeItem.Section(it) }),
        )
    }

    private suspend fun loadViews() {
        val items = repository
            .getUserViews()
            .filter { view -> CollectionType.fromString(view.collectionType?.serialName) in CollectionType.supported }
            .map { view -> view to repository.getLatestMedia(view.id) }
            .filter { (_, latest) -> latest.isNotEmpty() }
            .map { (view, latest) -> view.toView(latest) }
            .map { HomeItem.ViewItem(it) }
        _state.emit(
            _state.value.copy(views = items),
        )
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnRetryClick -> {
                loadData()
            }
            else -> Unit
        }
    }
}
