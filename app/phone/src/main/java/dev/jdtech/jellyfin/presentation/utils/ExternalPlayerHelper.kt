package dev.jdtech.jellyfin.presentation.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.player.local.domain.PlaylistManager
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExternalPlayerViewModel @Inject constructor(
    val appPreferences: AppPreferences,
    val playlistManager: PlaylistManager
) : ViewModel()

suspend fun launchExternalPlayerIfEnabled(
    context: Context,
    appPreferences: AppPreferences,
    playlistManager: PlaylistManager,
    itemId: UUID,
    itemKind: BaseItemKind,           // was String, now matches TV
    startFromBeginning: Boolean,
    launchInternalPlayer: () -> Unit
) {
    val useExternalPlayer = appPreferences.getValue(appPreferences.playerExternal)

    if (!useExternalPlayer) {
        launchInternalPlayer() // Fall back to the original Findroid player
        return
    }

    withContext(Dispatchers.IO) {
        try {
            val startItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = itemKind,  // was BaseItemKind.fromName(itemKind), now direct
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning,
            )

            if (startItem != null) {
                // FIX 1: Removed the redundant .toString()
                val videoUrl = startItem.mediaSourceUri

                // FIX 2: Reduced mapNotNull to map since 'uri' is never null
                val subUris = startItem.externalSubtitles.map { it.uri }.toTypedArray()

                val subNames = startItem.externalSubtitles.map {
                    it.title.ifBlank { it.language.ifBlank { "External Sub" } }
                }.toTypedArray()

                val title = if (startItem.parentIndexNumber != null && startItem.indexNumber != null) {
                    val identifier = if (startItem.indexNumberEnd == null) {
                        "S${startItem.parentIndexNumber}:E${startItem.indexNumber}"
                    } else {
                        "S${startItem.parentIndexNumber}:E${startItem.indexNumber}-${startItem.indexNumberEnd}"
                    }
                    if (startItem.seriesName != null) "${ startItem.seriesName} - $identifier" else identifier
                } else {
                    startItem.name
                }

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    // FIX 3: Replaced Uri.parse() with the KTX extension .toUri()
                    setDataAndType(videoUrl.toUri(), "video/*")

                    // pass filename/title to external player
                    putExtra("title", title)
                    // Pass playback position to external player (milliseconds)
                    putExtra("position", startItem.playbackPosition.toInt())

                    if (subUris.isNotEmpty()) {
                        val parcels = Array<android.os.Parcelable>(subUris.size) { subUris[it] }

                        // MX Player & JustPlayer
                        putExtra("subs", parcels)
                        putExtra("subs.name", subNames)
                        putExtra("subs.enable", arrayOf(parcels[0]))

                        // VLC
                        putExtra("subtitles_location", subUris[0].toString())
                    }
                }

                val chooserIntent = Intent.createChooser(intent, "Select Video Player").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not load media", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch external player")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error fetching media for external player", Toast.LENGTH_SHORT).show()
            }
        }
    }
}