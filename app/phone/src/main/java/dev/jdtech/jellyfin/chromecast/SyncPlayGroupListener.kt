package dev.jdtech.jellyfin.chromecast

import com.google.android.gms.cast.MediaInfo
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.GroupUpdateType
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.socket.SyncPlayCommandMessage
import org.jellyfin.sdk.model.socket.SyncPlayGroupUpdateMessage

class SyncPlayGroupListener(
    private val api: JellyfinApi, private val itemId: UUID,
) {
    val instance = api.api.ws()
    private var groupMessage: SyncPlayMedia? = null
    private var receivedPlaylist = false
    var receivedCommand = false
    val latestUpdate: Flow<SyncPlayMedia> = flow {

        while (true) {
                groupMessage = getMediaCommand(api, itemId)
            if (receivedCommand) {

                emit(groupMessage!!)
                receivedCommand = false
            }
            delay(500)
            }

        }


    suspend fun getMediaCommand (api: JellyfinApi, itemID: Any?): SyncPlayMedia? {
        var media : SyncPlayMedia ?=null
        var instance = api.api.ws()
        instance.addListener<SyncPlayCommandMessage> {
                message ->

            var isPlaying = true
            var command = ""
            if(message.command.command.equals(SendCommandType.UNPAUSE)){
                isPlaying = true
                receivedCommand = true
            }
            else if(message.command.command.equals(SendCommandType.PAUSE)){
                isPlaying = false
                receivedCommand = true
            }
            else if(message.command.command.equals(SendCommandType.SEEK)){
                isPlaying = false
                receivedCommand = true
            }

            media = SyncPlayMedia(itemId, message.command.positionTicks!!/ 10000, isPlaying)
        }

        instance.addListener<SyncPlayGroupUpdateMessage> {
                message ->
            if (message.update.type == GroupUpdateType.PLAY_QUEUE) {
                var Groupmessage = message.update.data
                receivedCommand = true
                var element = Groupmessage!!.jsonObject
                var startTime = element.get("StartPositionTicks").toString().toLong() / 10000
                var playList = element.get("Playlist")!!
                var ItemIdsArray = playList.jsonArray.get(0)
                var mediaIDString = ItemIdsArray.jsonObject.get("ItemId").toString()
                mediaIDString = mediaIDString.replace("\"", "")
                var r = Groupmessage.toString()
                var mediaInfo: MediaInfo? = null
                print(r + ItemIdsArray + mediaIDString)
                val regex = Regex("""([0-z]{8})([0-z]{4})([0-z]{4})([0-z]{4})([0-z]{12})""")
                var mediaId = regex.replace(mediaIDString) { match ->
                    "${match.groups[1]?.value}-${match.groups[2]?.value}-${match.groups[3]?.value}-${match.groups[4]?.value}-${match.groups[5]?.value}"
                }
                var ItemId = java.util.UUID.fromString(mediaId)
                var startPositionTicks = startTime.toInt()
                var hasItemId = true
                receivedCommand = true
                media = SyncPlayMedia(ItemId, startPositionTicks.toLong(), false)
            }

        }
        while(!receivedCommand){
            delay(100)
        }

        return media
    }

}
