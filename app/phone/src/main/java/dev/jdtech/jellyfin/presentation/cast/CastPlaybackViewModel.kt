package dev.jdtech.jellyfin.presentation.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.local.domain.PlaylistManager
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CastPlaybackViewModel @Inject constructor(
    val castManager: CastManager,
    private val playlistManager: PlaylistManager
) : ViewModel() {

    fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        viewModelScope.launch {
            val initialItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = BaseItemKind.fromName(itemKind),
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning
            )
            if (initialItem != null) {
                castManager.loadItem(initialItem, if (startFromBeginning) 0L else initialItem.playbackPosition)
            }
        }
    }
}
