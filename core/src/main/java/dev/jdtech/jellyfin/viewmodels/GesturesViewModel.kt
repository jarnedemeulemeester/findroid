package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Gesture
import dev.jdtech.jellyfin.models.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GesturesViewModel
@Inject
constructor(
    private val database: ServerDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val gestures: List<Gesture>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    private val eventsChannel = Channel<GesturesEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    private var currentProfileId: String = ""

    fun loadGestures(profileId: String) {
        currentProfileId = profileId
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                // val gestures = database.getGestures(profileId)
                // _uiState.emit(UiState.Normal(gestures))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }
}

sealed interface GesturesEvent {
    data object NavigateToHome : GesturesEvent
}
