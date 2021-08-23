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
class FavoriteViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val _favoriteSections = MutableLiveData<List<FavoriteSection>>()
    val favoriteSections: LiveData<List<FavoriteSection>> = _favoriteSections

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadData()
    }

    fun loadData() {
        _error.value = null
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                val items = jellyfinRepository.getFavoriteItems()

                if (items.isEmpty()) {
                    _favoriteSections.value = listOf()
                    _finishedLoading.value = true
                    return@launch
                }

                val tempFavoriteSections = mutableListOf<FavoriteSection>()

                withContext(Dispatchers.Default) {
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.type == "Movie" }).let {
                        if (it.items.isNotEmpty()) tempFavoriteSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Shows",
                        items.filter { it.type == "Series" }).let {
                        if (it.items.isNotEmpty()) tempFavoriteSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Episodes",
                        items.filter { it.type == "Episode" }).let {
                        if (it.items.isNotEmpty()) tempFavoriteSections.add(
                            it
                        )
                    }
                }

                _favoriteSections.value = tempFavoriteSections
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.toString()
            }
            _finishedLoading.value = true
        }
    }
}