package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SeasonViewModel
@Inject
constructor(private val jellyfinRepository: JellyfinRepository) : ViewModel() {

    private val _episodes = MutableLiveData<List<BaseItemDto>>()
    val episodes: LiveData<List<BaseItemDto>> = _episodes

    fun loadEpisodes(seriesId: UUID, seasonId: UUID) {
        viewModelScope.launch {
            _episodes.value = jellyfinRepository.getEpisodes(seriesId, seasonId, fields = listOf(ItemFields.OVERVIEW))
        }
    }
}