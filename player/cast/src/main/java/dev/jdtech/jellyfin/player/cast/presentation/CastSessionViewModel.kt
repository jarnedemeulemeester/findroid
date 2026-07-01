package dev.jdtech.jellyfin.player.cast.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.player.cast.CastPlayerController
import dev.jdtech.jellyfin.player.cast.CastSessionManager
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.model.UUID
import javax.inject.Inject

@HiltViewModel
class CastSessionViewModel @Inject constructor(
    sessionManager: CastSessionManager,
    private val playerController: CastPlayerController
) : ViewModel() {

    val connectionState: StateFlow<CastConnectionState> = sessionManager.connectionState

    fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        playerController.playItem(itemId, itemKind, startFromBeginning)
    }
}