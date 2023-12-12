package dev.jdtech.jellyfin.chromecast

import com.google.android.gms.cast.MediaInfo
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.model.api.GroupUpdateType
import org.jellyfin.sdk.model.socket.SyncPlayGroupUpdateMessage

class SyncPlayDataSource(
    private val api: JellyfinApi
) {
    val instance = api.api.ws()
    private var groupMessage: JsonElement? = null
    private var receivedPlaylist = false
    val latestUpdate: Flow<SyncPlayMedia> = flow {

        var gotItemId = false
        while (!gotItemId) {
            groupMessage = getDataSource(api)
            if (receivedPlaylist) {

                var Groupmessage = groupMessage
                print(Groupmessage)
                var element = Groupmessage!!.jsonObject
                var startTime = element.get("StartPositionTicks").toString().toLong() / 10000
                var playList = element.get("Playlist")!!
                var ItemIdsArray = playList.jsonArray.get(0)
                var mediaIDString = ItemIdsArray.jsonObject.get("ItemId").toString()
                mediaIDString = mediaIDString.replace("\"", "")
                var playListItemID = ItemIdsArray.jsonObject.get("PlaylistItemId").toString()
                playListItemID = playListItemID.replace("\"", "")
                var r = Groupmessage.toString()
                var mediaInfo: MediaInfo? = null
                print(r + ItemIdsArray + mediaIDString)
                val regex = Regex("""([0-z]{8})([0-z]{4})([0-z]{4})([0-z]{4})([0-z]{12})""")
                var mediaId = regex.replace(mediaIDString) { match ->
                    "${match.groups[1]?.value}-${match.groups[2]?.value}-${match.groups[3]?.value}-${match.groups[4]?.value}-${match.groups[5]?.value}"
                }
                playListItemID = regex.replace(playListItemID) { match ->
                    "${match.groups[1]?.value}-${match.groups[2]?.value}-${match.groups[3]?.value}-${match.groups[4]?.value}-${match.groups[5]?.value}"
                }
                var ItemId = java.util.UUID.fromString(mediaId)
                var startPositionTicks = startTime.toInt()
                var hasItemId = true
                var syncItem = SyncPlayMedia(
                    ItemId,
                    startPositionTicks.toLong(),
                    hasItemId,
                    playListItemID
                )

                emit(syncItem!!)
                gotItemId = true
            }
            delay(500)
        }
    }

    suspend fun getDataSource (api: JellyfinApi): JsonElement? {
        var instance = api.api.ws()

        var groupUpdateMessage: SyncPlayGroupUpdateMessage
        instance.addListener<SyncPlayGroupUpdateMessage> {
                message ->
            if (message.update.type == GroupUpdateType.PLAY_QUEUE) {
                groupMessage = message.update.data
                receivedPlaylist = true
            }

        }
       while(!receivedPlaylist){
           delay(100)
       }
        return groupMessage
    }
}
