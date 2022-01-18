package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.adapters.DownloadEpisodeItem
import dev.jdtech.jellyfin.adapters.EpisodeItem
import dev.jdtech.jellyfin.models.DownloadMetadata
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DownloadSeriesViewModel
@Inject
constructor() : ViewModel() {

    private val _downloadEpisodes = MutableLiveData<List<DownloadEpisodeItem>>()
    val downloadEpisodes: LiveData<List<DownloadEpisodeItem>> = _downloadEpisodes

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadEpisodes(seriesMetadata: DownloadSeriesMetadata) {
        _error.value = null
        _finishedLoading.value = false
        try {
            _downloadEpisodes.value = getEpisodes(seriesMetadata)
        } catch (e: Exception) {
            Timber.e(e)
            _error.value = e.toString()
        }
        _finishedLoading.value = true
    }

    private fun getEpisodes(seriesMetadata: DownloadSeriesMetadata): List<DownloadEpisodeItem> {
        val episodes = seriesMetadata.episodes
        return listOf(DownloadEpisodeItem.Header) + episodes.sortedWith(compareBy( {it.metadata!!.parentIndexNumber}, {it.metadata!!.indexNumber} )).map { DownloadEpisodeItem.Episode(it) }
    }
}