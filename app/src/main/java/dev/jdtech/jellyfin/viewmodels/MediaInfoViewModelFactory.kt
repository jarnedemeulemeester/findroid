package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException
import java.util.*

class MediaInfoViewModelFactory(
    private val application: Application,
    private val itemId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaInfoViewModel(application, itemId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}