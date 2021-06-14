package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class MediaViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}