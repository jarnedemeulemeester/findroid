package dev.jdtech.jellyfin.serverselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.lang.IllegalArgumentException

class ServerSelectViewModelFactory(
    private val dataSource: ServerDatabaseDao
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServerSelectViewModel::class.java)) {
            return ServerSelectViewModel(dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}