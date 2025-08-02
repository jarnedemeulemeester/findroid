package dev.jdtech.jellyfin.film.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
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
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import dev.jdtech.jellyfin.film.R as FilmR

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    val repository: JellyfinRepository,
    val appPreferences: AppPreferences,
    val database: ServerDatabaseDao,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val uuidSuggestions = UUID.fromString("31e47044-9b79-4bb0-99d0-0e477ed65420")
    private val uuidContinueWatching = UUID(4937169328197226115, -4704919157662094443) // 44845958-8326-4e83-beb4-c4f42e9eeb95
    private val uuidNextUp = UUID(1783371395749072194, -6164625418200444295) // 18bfced5-f237-4d42-aa72-d9d7fed19279

    private val uiTextContinueWatching = UiText.StringResource(FilmR.string.continue_watching)
    private val uiTextNextUp = UiText.StringResource(FilmR.string.next_up)

    fun loadData() {
        Timber.i("Loading data")
        viewModelScope.launch(Dispatchers.Default) {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                appPreferences.getValue(appPreferences.currentServer)?.let { serverId ->
                    loadServerName(serverId)
                }
                if (appPreferences.getValue(appPreferences.offlineMode)) _state.emit(_state.value.copy(isOffline = true))

                loadSuggestions()
                loadResumeItems()
                loadNextUpItems()
                loadViews()
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
            _state.emit(_state.value.copy(isLoading = false))
        }
    }

    private suspend fun loadServerName(serverId: String) {
        val server = database.get(serverId)
        if (server != null) {
            _state.emit(_state.value.copy(server = server))
        }
    }

    private suspend fun loadSuggestions() {
        Timber.i("Loading suggestions")
        if (!appPreferences.getValue(appPreferences.homeSuggestions)) {
            _state.emit(
                _state.value.copy(suggestionsSection = null),
            )
            return
        }

        val items = repository.getSuggestions()

        val section = if (items.isEmpty()) {
            null
        } else {
            HomeItem.Suggestions(
                id = uuidSuggestions,
                items = items,
            )
        }

        _state.emit(
            _state.value.copy(suggestionsSection = section),
        )
    }

    private suspend fun loadResumeItems() {
        Timber.i("Loading resume items")
        if (!appPreferences.getValue(appPreferences.homeContinueWatching)) {
            _state.emit(
                _state.value.copy(resumeSection = null),
            )
            return
        }

        val resumeItems = repository.getResumeItems()

        val section = if (resumeItems.isEmpty()) {
            null
        } else {
            HomeItem.Section(
                HomeSection(
                    uuidContinueWatching,
                    uiTextContinueWatching,
                    resumeItems,
                ),
            )
        }

        _state.emit(
            _state.value.copy(resumeSection = section),
        )
    }

    private suspend fun loadNextUpItems() {
        Timber.i("Loading next up items")
        if (!appPreferences.getValue(appPreferences.homeNextUp)) {
            _state.emit(
                _state.value.copy(nextUpSection = null),
            )
            return
        }

        val nextUpItems = repository.getNextUp()

        val section = if (nextUpItems.isEmpty()) {
            null
        } else {
            HomeItem.Section(
                HomeSection(
                    uuidNextUp,
                    uiTextNextUp,
                    nextUpItems,
                ),
            )
        }

        _state.emit(
            _state.value.copy(nextUpSection = section),
        )
    }

    private suspend fun loadViews() {
        Timber.i("Loading views")
        val items = if (appPreferences.getValue(appPreferences.homeLatest)) {
            repository
                .getUserViews()
                .filter { view -> CollectionType.fromString(view.collectionType?.serialName) in CollectionType.supported }
                .map { view -> view to repository.getLatestMedia(view.id) }
                .filter { (_, latest) -> latest.isNotEmpty() }
                .map { (view, latest) -> view.toView(latest) }
                .map { HomeItem.ViewItem(it) }
        } else {
            emptyList()
        }

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
