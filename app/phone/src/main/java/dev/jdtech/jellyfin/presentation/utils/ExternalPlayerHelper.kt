package dev.jdtech.jellyfin.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.player.local.domain.PlaylistManager
import dev.jdtech.jellyfin.repository.JellyfinRepository
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
    val playlistManager: PlaylistManager,
    private val repository: JellyfinRepository,
) : ViewModel() {

    suspend fun reportStop(itemId: UUID, positionMs: Long) {
        try {
            repository.postPlaybackStop(
                itemId = itemId,
                positionTicks = positionMs.times(10000),
                playedPercentage = 0,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playback stop after external player")
        }
    }
}
suspend fun launchExternalPlayerIfEnabled(
    context: Context,
    appPreferences: AppPreferences,
    playlistManager: PlaylistManager,
    itemId: UUID,
    itemKind: BaseItemKind,
    startFromBeginning: Boolean,
    externalPlayerLauncher: ActivityResultLauncher<Intent>,
    launchInternalPlayer: () -> Unit
) {
    val useExternalPlayer = appPreferences.getValue(appPreferences.playerExternal)

    if (!useExternalPlayer) {
        launchInternalPlayer()
        return
    }

    withContext(Dispatchers.IO) {
        try {
            val startItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = itemKind,
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning,
            )

            if (startItem != null) {
                val videoUrl = startItem.mediaSourceUri
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
                    if (startItem.seriesName != null) "${startItem.seriesName} - $identifier" else identifier
                } else {
                    startItem.name
                }

                val pm = context.packageManager
                val mxProPackage = "com.mxtech.videoplayer.pro"
                val mxFreePackage = "com.mxtech.videoplayer.ad"
                val vlcPackage = "org.videolan.vlc"
                val mpvPackage = "is.xyz.mpv"

                val isMxPro = isPackageInstalled(pm, mxProPackage)
                val isMxFree = isPackageInstalled(pm, mxFreePackage)
                val isVlc = isPackageInstalled(pm, vlcPackage)
                val isMpv = isPackageInstalled(pm, mpvPackage)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(videoUrl.toUri(), "video/*")
                    putExtra("return_result", true)
                    putExtra("secure_uri", true)
                    putExtra("title", title)
                    putExtra("position", startItem.playbackPosition.toInt())
                    putExtra("subtitles_location", subUris.firstOrNull()?.toString())

                    if (subUris.isNotEmpty()) {
                        val parcels = Array<Parcelable>(subUris.size) { subUris[it] }
                        putExtra("subs", parcels)
                        putExtra("subs.name", subNames)
                        putExtra("subs.enable", arrayOf(parcels[0]))
                    }

                    when {
                        isMxPro -> setClassName(mxProPackage, "$mxProPackage.ActivityScreen")
                        isMxFree -> setClassName(mxFreePackage, "$mxFreePackage.ActivityScreen")
                        isVlc -> setClassName(vlcPackage, "$vlcPackage.StartActivity")
                        isMpv -> setClassName(mpvPackage, "$mpvPackage.MPVActivity")
                    }
                }

                withContext(Dispatchers.Main) {
                    externalPlayerLauncher.launch(intent)
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

@Composable
fun rememberExternalPlayerLauncher(
    onResult: (positionMs: Long?) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data

        val positionMs: Long? = when (data?.action) {
            "com.mxtech.intent.result.VIEW" -> {
                if (resultCode == Activity.RESULT_OK) {
                    when (data.getStringExtra("end_by")) {
                        "user" -> {
                            val pos = data.getIntExtra("position", -1)
                            if (pos > 0) pos.toLong() else null
                        }
                        "playback_completion" -> null
                        else -> null
                    }
                } else null
            }
            "org.videolan.vlc.player.result" -> {
                if (resultCode == Activity.RESULT_OK) {
                    val pos = data.getLongExtra("extra_position", -1L)
                    val dur = data.getLongExtra("extra_duration", -1L)
                    if (pos > 0L && pos != dur) pos else null
                } else null
            }
            "is.xyz.mpv.result" -> {
                if (resultCode == Activity.RESULT_OK) {
                    val pos = data.getIntExtra("position", -1)
                    if (pos > 0) pos.toLong() else null
                } else null
            }
            else -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.getIntExtra("position", -1)?.takeIf { it > 0 }?.toLong()
                } else null
            }
        }

        // Pass the parsed result back to whatever screen called this
        onResult(positionMs)
    }
}

private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
    return try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}