package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.adapters.EpisodeItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SeasonViewModel
@Inject
constructor(private val jellyfinRepository: JellyfinRepository) : ViewModel() {

    private val _episodes = MutableLiveData<List<EpisodeItem>>()
    val episodes: LiveData<List<EpisodeItem>> = _episodes

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadEpisodes(seriesId: UUID, seasonId: UUID) {
        _error.value = null
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                _episodes.value = getEpisodes(seriesId, seasonId)
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.message
            }
            _finishedLoading.value = true
        }
    }

    private suspend fun getEpisodes(seriesId: UUID, seasonId: UUID): List<EpisodeItem> {
        val episodes = jellyfinRepository.getEpisodes(seriesId, seasonId, fields = listOf(ItemFields.OVERVIEW))
        return listOf(EpisodeItem.Header) + episodes.map { EpisodeItem.Episode(it) }
    }
}