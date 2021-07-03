package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException
import java.util.*

class EpisodeBottomSheetViewModelFactory(
    private val application: Application,
    private val episodeId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EpisodeBottomSheetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EpisodeBottomSheetViewModel(application, episodeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}