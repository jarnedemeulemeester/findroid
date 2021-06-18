package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.*
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

class MediaViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

    private val _collections = MutableLiveData<List<BaseItemDto>>()
    val collections: LiveData<List<BaseItemDto>> = _collections

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    init {
        viewModelScope.launch {
            val items = getItems(jellyfinApi.userId!!)
            _collections.value = items
            _finishedLoading.value = true
        }
    }

    private suspend fun getItems(userId: UUID): List<BaseItemDto>? {
        var items: List<BaseItemDto>?
        withContext(Dispatchers.IO) {
            items = jellyfinApi.itemsApi.getItems(userId).content.items
        }
        return items
    }
}