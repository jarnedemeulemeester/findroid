package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val database: ServerDatabaseDao,
) : ViewModel() {
    private val navigateToAddServer = MutableSharedFlow<Boolean>()

    fun onNavigateToAddServer(scope: LifecycleCoroutineScope, collector: (Boolean) -> Unit) {
        scope.launch { navigateToAddServer.collect { collector(it) } }
    }

    init {
        Timber.d("Start Main")
        viewModelScope.launch {
            val servers: List<Server>
            withContext(Dispatchers.IO) {
                servers = database.getAllServersSync()
            }
            if (servers.isEmpty()) {
                navigateToAddServer.emit(true)
            }
        }
    }
}