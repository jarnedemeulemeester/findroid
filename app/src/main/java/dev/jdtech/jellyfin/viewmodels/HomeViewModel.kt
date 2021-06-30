package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.*
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.NextUp
import dev.jdtech.jellyfin.models.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import java.util.*

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

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
                val viewsResult = getViews(jellyfinApi.userId!!)
                for (view in viewsResult.items!!) {
                    val latestItems = getLatestMedia(jellyfinApi.userId!!, view.id)
                    if (latestItems.isEmpty()) continue
                    val v = view.toView()
                    v.items = latestItems
                    views.add(v)
                }

                val nextUpItems = getNextUp()
                val nextUp = NextUp(UUID.randomUUID(), nextUpString, nextUpItems)

                _views.value = when (nextUpItems) {
                    null -> views.map { HomeItem.ViewItem(it) }
                    else -> listOf(HomeItem.NextUpSection(nextUp)) + views.map { HomeItem.ViewItem(it) }
                }

                _finishedLoading.value = true
            } catch (e: Exception) {
                _finishedLoading.value = true
                _error.value = true
            }
        }
    }

    private suspend fun getViews(userId: UUID): BaseItemDtoQueryResult {
        val views: BaseItemDtoQueryResult
        withContext(Dispatchers.IO) {
            views = jellyfinApi.viewsApi.getUserViews(userId).content
        }
        return views
    }

    private suspend fun getLatestMedia(userId: UUID, parentId: UUID): List<BaseItemDto> {
        val items: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            items = jellyfinApi.userLibraryApi.getLatestMedia(userId, parentId = parentId).content
        }
        return items
    }

    private suspend fun getNextUp(): List<BaseItemDto>? {
        val items: List<BaseItemDto>?
        withContext(Dispatchers.IO) {
            items = jellyfinApi.showsApi.getNextUp(jellyfinApi.userId!!).content.items
        }
        return items
    }

}

private fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name
    )
}
