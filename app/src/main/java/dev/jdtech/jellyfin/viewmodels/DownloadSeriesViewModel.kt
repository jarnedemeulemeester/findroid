package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.adapters.DownloadEpisodeItem
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
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
        return listOf(DownloadEpisodeItem.Header) + episodes.sortedWith(compareBy( {it.item!!.parentIndexNumber}, {it.item!!.indexNumber} )).map { DownloadEpisodeItem.Episode(it) }
    }
}