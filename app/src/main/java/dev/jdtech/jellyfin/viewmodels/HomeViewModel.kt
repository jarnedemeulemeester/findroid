package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.adapters.HomeItem.Section
import dev.jdtech.jellyfin.adapters.HomeItem.ViewItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.unsupportedCollections
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.syncPlaybackProgress
import dev.jdtech.jellyfin.utils.toView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject internal constructor(
    private val application: Application,
    private val repository: JellyfinRepository
) : ViewModel() {

    private val views = MutableLiveData<List<HomeItem>>()
    private val state = MutableSharedFlow<State>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        loadData(updateCapabilities = true)
    }

    private val continueWatchingString = application.resources.getString(R.string.continue_watching)
    private val nextUpString = application.resources.getString(R.string.next_up)

    fun views(): LiveData<List<HomeItem>> = views

    fun onStateUpdate(
        scope: LifecycleCoroutineScope,
        collector: (State) -> Unit
    ) {
        scope.launch { state.collect { collector(it) } }
    }

    fun refreshData() = loadData(updateCapabilities = false)

    private fun loadData(updateCapabilities: Boolean) {
        state.tryEmit(Loading(inProgress = true))

        viewModelScope.launch {
            try {
                if (updateCapabilities) repository.postCapabilities()

                val updated = loadDynamicItems() + loadViews()
                views.postValue(updated)

                withContext(Dispatchers.Default) {
                    syncPlaybackProgress(repository, application)
                }
                state.tryEmit(Loading(inProgress = false))
            } catch (e: Exception) {
                Timber.e(e)
                state.tryEmit(LoadingError(e.toString()))
            }
        }
    }

    private suspend fun loadDynamicItems() = withContext(Dispatchers.IO) {
        val resumeItems = repository.getResumeItems()
        val nextUpItems = repository.getNextUp()

        val items = mutableListOf<HomeSection>()
        if (resumeItems.isNotEmpty()) {
            items.add(HomeSection(continueWatchingString, resumeItems))
        }

        if (nextUpItems.isNotEmpty()) {
            items.add(HomeSection(nextUpString, nextUpItems))
        }

        items.map { Section(it) }
    }

    private suspend fun loadViews() = withContext(Dispatchers.IO) {
        repository
            .getUserViews()
            .filter { view -> unsupportedCollections().none { it.type == view.collectionType } }
            .map { view -> view to repository.getLatestMedia(view.id) }
            .filter { (_, latest) -> latest.isNotEmpty() }
            .map { (view, latest) -> view.toView().apply { items = latest } }
            .map { ViewItem(it) }
    }

    sealed class State

    data class LoadingError(val message: String) : State()
    data class Loading(val inProgress: Boolean) : State()
}


