package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    application: Application,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val continueWatchingString = application.resources.getString(R.string.continue_watching)
    private val nextUpString = application.resources.getString(R.string.next_up)

    private val _views = MutableLiveData<List<HomeItem>>()
    val views: LiveData<List<HomeItem>> = _views

    private val _items = MutableLiveData<List<BaseItemDto>>()
    val items: LiveData<List<BaseItemDto>> = _items

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<Boolean>()
    val error: LiveData<Boolean> = _error

    init {
        loadData()
    }

    fun loadData() {
        _error.value = false
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                val views: MutableList<View> = mutableListOf()
                val userViews = jellyfinRepository.getUserViews()
                for (view in userViews) {
                    val latestItems = jellyfinRepository.getLatestMedia(view.id)
                    if (latestItems.isEmpty()) continue
                    val v = view.toView()
                    v.items = latestItems
                    views.add(v)
                }

                val items = mutableListOf<HomeItem>()

                val resumeItems = jellyfinRepository.getResumeItems()
                val resumeSection =
                    HomeSection(UUID.randomUUID(), continueWatchingString, resumeItems)

                if (!resumeItems.isNullOrEmpty()) {
                    items.add(HomeItem.Section(resumeSection))
                }

                val nextUpItems = jellyfinRepository.getNextUp()
                val nextUpSection = HomeSection(UUID.randomUUID(), nextUpString, nextUpItems)

                if (!nextUpItems.isNullOrEmpty()) {
                    items.add(HomeItem.Section(nextUpSection))
                }

                _views.value = items + views.map { HomeItem.ViewItem(it) }

                _finishedLoading.value = true
            } catch (e: Exception) {
                Log.e("HomeViewModel", e.message.toString())
                _finishedLoading.value = true
                _error.value = true
            }
        }
    }
}

private fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name
    )
}
