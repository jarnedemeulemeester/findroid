package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
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

    init {
        viewModelScope.launch {
            val items = jellyfinRepository.getItems()
            _collections.value =
                items.filter {
                    it.collectionType != "homevideos" &&
                            it.collectionType != "music" &&
                            it.collectionType != "playlists" &&
                            it.collectionType != "boxsets"
                }
            _finishedLoading.value = true
        }
    }
}