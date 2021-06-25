package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import java.util.*

class SeasonViewModel(application: Application, seriesId: UUID, seasonId: UUID) :
    AndroidViewModel(application) {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

    private val _episodes = MutableLiveData<List<BaseItemDto>>()
    val episodes: LiveData<List<BaseItemDto>> = _episodes

    init {
        viewModelScope.launch {
            _episodes.value = getEpisodes(seriesId, seasonId)
        }
    }

    private suspend fun getEpisodes(seriesId: UUID, seasonId: UUID): List<BaseItemDto>? {
        val episodes: List<BaseItemDto>?
        withContext(Dispatchers.IO) {
            episodes = jellyfinApi.showsApi.getEpisodes(
                seriesId, jellyfinApi.userId!!, seasonId = seasonId, fields = listOf(
                    ItemFields.OVERVIEW
                )
            ).content.items
        }
        return episodes
    }
}