package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MediaViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val _collections = MutableLiveData<List<BaseItemDto>>()
    val collections: LiveData<List<BaseItemDto>> = _collections

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadData()
    }

    fun loadData() {
        _finishedLoading.value = false
        _error.value = null
        viewModelScope.launch {
            try {
                val items = jellyfinRepository.getItems()
                _collections.value =
                    items.filter {
                        it.collectionType != "homevideos" &&
                                it.collectionType != "music" &&
                                it.collectionType != "playlists" &&
                                it.collectionType != "boxsets"
                    }
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.message
            }
            _finishedLoading.value = true
        }
    }
}