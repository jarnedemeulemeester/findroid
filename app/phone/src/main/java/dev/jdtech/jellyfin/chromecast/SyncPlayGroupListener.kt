package dev.jdtech.jellyfin.chromecast

import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.model.socket.SyncPlayCommandMessage

class SyncPlayGroupListener(
    private val api: JellyfinApi
) {
    val instance = api.api.ws()
    private var groupMessage: SyncPlayCommandMessage? = null
    private var receivedPlaylist = false
    var receivedCommand = false
    val latestUpdate: Flow<SyncPlayCommandMessage> = flow {

        while (true) {
                groupMessage = getMediaCommand(api)
            if (receivedCommand) {
                emit(groupMessage!!)
                receivedCommand = false
            }
            delay(500)
            }

        }


    suspend fun getMediaCommand (api: JellyfinApi): SyncPlayCommandMessage? {
        var message = SyncPlayCommandMessage
        var instance = api.api.ws()
        instance.addListener<SyncPlayCommandMessage> {
                message ->
            groupMessage = message
            receivedCommand = true

        }
        while(!receivedCommand){
            delay(100)
        }

        return groupMessage
    }

}
