package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
@Inject
constructor(private val jellyfinRepository: JellyfinRepository) : ViewModel() {

    private val _items = MutableLiveData<List<BaseItemDto>>()
    val items: LiveData<List<BaseItemDto>> = _items

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadItems(parentId: UUID) {
        _error.value = null
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                _items.value = jellyfinRepository.getItems(parentId)
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.message
            }
            _finishedLoading.value = true
        }
    }
}