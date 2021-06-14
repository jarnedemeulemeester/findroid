package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.models.ViewItem
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

class HomeViewModel(
    application: Application
) : ViewModel() {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

    private val _views = MutableLiveData<List<View>>()
    val views: LiveData<List<View>> = _views

    private val _items = MutableLiveData<List<BaseItemDto>>()
    val items: LiveData<List<BaseItemDto>> = _items

    init {
        viewModelScope.launch {
            val views: MutableList<View> = mutableListOf()

            val result by jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!)

            for (view in result.items!!) {
                val items: MutableList<ViewItem> = mutableListOf()
                val resultItems by jellyfinApi.userLibraryApi.getLatestMedia(jellyfinApi.userId!!, parentId = view.id)
                if (resultItems.isEmpty()) continue
                val v = view.toView()
                for (item in resultItems) {
                    val i = jellyfinApi.api.baseUrl?.let { item.toViewItem(it) }
                    if (i != null) {
                        items.add(i)
                    }
                }
                v.items = items
                views.add(v)
            }

            _views.value = views

        }
    }

}

private fun BaseItemDto.toViewItem(baseUrl: String) : ViewItem {
    return when (type) {
        "Episode" -> ViewItem(
            id = seriesId!!,
            name = seriesName,
            primaryImageUrl = baseUrl.plus("/items/${seriesId}/Images/Primary")
        )
        else -> ViewItem(
            id = id,
            name = name,
            primaryImageUrl = baseUrl.plus("/items/${id}/Images/Primary")
        )
    }
}

private fun BaseItemDto.toView() : View {
    return View(
        id = id,
        name = name
    )
}
