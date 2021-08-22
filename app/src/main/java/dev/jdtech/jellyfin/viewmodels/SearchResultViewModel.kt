package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val _sections = MutableLiveData<List<FavoriteSection>>()
    val sections: LiveData<List<FavoriteSection>> = _sections

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadData(query: String) {
        _error.value = null
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                val items = jellyfinRepository.getSearchItems(query)

                if (items.isEmpty()) {
                    _sections.value = listOf()
                    _finishedLoading.value = true
                    return@launch
                }

                val tempSections = mutableListOf<FavoriteSection>()

                withContext(Dispatchers.Default) {
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.type == "Movie" }).let {
                        if (it.items.isNotEmpty()) tempSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Shows",
                        items.filter { it.type == "Series" }).let {
                        if (it.items.isNotEmpty()) tempSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Episodes",
                        items.filter { it.type == "Episode" }).let {
                        if (it.items.isNotEmpty()) tempSections.add(
                            it
                        )
                    }
                }

                _sections.value = tempSections
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.message
            }
            _finishedLoading.value = true
        }
    }
}