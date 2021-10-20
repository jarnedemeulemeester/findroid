package dev.jdtech.jellyfin.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.DownloadSection
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.loadDownloadedEpisodes
import dev.jdtech.jellyfin.utils.postDownloadPlaybackProgress
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel
@Inject
constructor(
    private val application: Application,
) : ViewModel() {
    private val _downloadSections = MutableLiveData<List<DownloadSection>>()
    val downloadSections: LiveData<List<DownloadSection>> = _downloadSections

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadData()
    }

    @SuppressLint("ResourceType")
    fun loadData() {
        _error.value = null
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                val items = loadDownloadedEpisodes(application)
                if (items.isEmpty()) {
                    _downloadSections.value = listOf()
                    _finishedLoading.value = true
                    return@launch
                }
                val tempDownloadSections = mutableListOf<DownloadSection>()
                    withContext(Dispatchers.Default) {
                        DownloadSection(
                            UUID.randomUUID(),
                            "Episodes",
                            items.filter { it.metadata?.type == "Episode"}).let {
                            if (it.items.isNotEmpty()) tempDownloadSections.add(
                                it
                            )
                        }
                    DownloadSection(
                            UUID.randomUUID(),
                            "Movies",
                            items.filter { it.metadata?.type == "Movie" }).let {
                            if (it.items.isNotEmpty()) tempDownloadSections.add(
                                it
                            )
                        }
                    }
                _downloadSections.value = tempDownloadSections
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.toString()
            }
            _finishedLoading.value = true
        }
    }
}