package dev.jdtech.jellyfin.chromecast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.framework.CastSession
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.JoinGroupRequestDto

data class SyncPlayMedia(
    var itemID: UUID,
    var timestamp: Long,
    var isPlaying: Boolean
)
class SyncPlayCast internal constructor(private val repository: JellyfinRepository) {

    private val progressListeners: MutableList<PlayerViewModel.MyProgressListener> = mutableListOf()


    companion object {
        suspend fun startCast(
            api: JellyfinApi,
            get: UUID,
            context: Context,
            repository: JellyfinRepository,
            groupJoinRequest: JoinGroupRequestDto
        ) {









        }


        suspend fun getStreamCastUrlAndPrint(itemId: UUID, media: String, repository: JellyfinRepository): String {
            val url = repository.getStreamCastUrl(itemId, media)
            print(url)
            return url
        }


        private fun loadRemoteMedia(
            position: Int,
            mCastSession: CastSession,
            mediaInfo: MediaInfo,
            streamUrl: String,

            ) {

                if (mCastSession == null) {
                    return
                }


                val remoteMediaClient = mCastSession.remoteMediaClient ?: return
                var previousSubtitleTrackIds: LongArray? = null
                var newIndex = -1
                var subtitleIndex = -1
                var newAudioIndex = 1




                remoteMediaClient.load(
                    MediaLoadRequestData.Builder()
                        .setMediaInfo(mediaInfo)
                        .setAutoplay(true)
                        .setCurrentTime(position.toLong()).build(),
                )
                val mediaStatus = remoteMediaClient.mediaStatus
                val activeMediaTracks = mediaStatus?.activeTrackIds
            }

    }
}