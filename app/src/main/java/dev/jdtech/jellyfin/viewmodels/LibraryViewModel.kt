package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.*
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

class LibraryViewModel(application: Application, userId: UUID) : AndroidViewModel(application) {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

    private val _items = MutableLiveData<List<BaseItemDto>>()
    val items: LiveData<List<BaseItemDto>> = _items

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    init {
        viewModelScope.launch {
            _items.value = getItems(jellyfinApi.userId!!, userId)
            _finishedLoading.value = true
        }
    }

    private suspend fun getItems(userId: UUID, parentId: UUID): List<BaseItemDto> {
        val items: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            items = jellyfinApi.itemsApi.getItems(userId, parentId = parentId).content.items!!
        }
        return items
    }
}