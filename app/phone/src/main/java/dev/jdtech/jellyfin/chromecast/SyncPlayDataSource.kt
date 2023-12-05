package dev.jdtech.jellyfin.chromecast

import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement
import org.jellyfin.sdk.api.sockets.addGeneralCommandsListener
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.api.sockets.addPlayStateCommandsListener
import org.jellyfin.sdk.model.api.GroupUpdateType
import org.jellyfin.sdk.model.socket.GeneralCommandMessage
import org.jellyfin.sdk.model.socket.PlayMessage
import org.jellyfin.sdk.model.socket.PlayStateMessage
import org.jellyfin.sdk.model.socket.SessionsMessage
import org.jellyfin.sdk.model.socket.SyncPlayCommandMessage
import org.jellyfin.sdk.model.socket.SyncPlayGroupUpdateMessage

class SyncPlayDataSource(
    private val api: JellyfinApi
) {
    val instance = api.api.ws()
    private var groupMessage: JsonElement? = null
    private var receivedPlaylist = false
    val latestUpdate: Flow<JsonElement> = flow {
        instance.addGeneralCommandsListener { message ->
            // type of message is GeneralCommandMessage
            println("Received a message: $message")
        }

        instance.addPlayStateCommandsListener { message ->
            // type of message is PlayStateMessage
            println("Received a message: $message")
        }

        instance.addListener<PlayMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<PlayStateMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<SyncPlayCommandMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<SessionsMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<GeneralCommandMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        while (true) {
            groupMessage = getDataSource(api)
            if (receivedPlaylist) {
                emit(groupMessage!!)
                receivedPlaylist = false
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
            }
            receivedPlaylist = true
        }
       while(!receivedPlaylist){
           delay(100)
       }
        return groupMessage
    }
}
