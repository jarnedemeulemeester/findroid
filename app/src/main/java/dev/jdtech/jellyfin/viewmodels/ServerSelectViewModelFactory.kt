package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.lang.IllegalArgumentException

class ServerSelectViewModelFactory(
    private val dataSource: ServerDatabaseDao,
    private val application: Application
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServerSelectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServerSelectViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}