package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException
import java.util.*

class SeasonViewModelFactory(
    private val application: Application,
    private val seriesId: UUID,
    private val seasonId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeasonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeasonViewModel(application, seriesId, seasonId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}