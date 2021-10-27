package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.SortBy
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.util.UUID
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

    fun loadItems(
        parentId: UUID,
        libraryType: String?,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING
    ) {
        _error.value = null
        _finishedLoading.value = false
        Timber.d("$libraryType")
        val itemType = when (libraryType) {
            "movies" -> "Movie"
            "tvshows" -> "Series"
            "boxsets" -> "BoxSet"
            else -> null
        }
        viewModelScope.launch {
            try {
                _items.value = jellyfinRepository.getItems(
                    parentId,
                    includeTypes = if (itemType != null) listOf(itemType) else null,
                    recursive = true,
                    sortBy = sortBy,
                    sortOrder = sortOrder
                )
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.toString()
            }
            _finishedLoading.value = true
        }
    }
}