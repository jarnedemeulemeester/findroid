package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto

class HomeViewModel(
    application: Application
) : ViewModel() {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

    private val _views = MutableLiveData<List<BaseItemDto>>()
    val views: LiveData<List<BaseItemDto>> = _views

    init {
        viewModelScope.launch {
            val result by jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!)
            _views.value = result.items
        }
    }

}