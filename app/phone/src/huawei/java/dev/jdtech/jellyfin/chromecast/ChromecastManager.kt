package dev.jdtech.jellyfin.chromecast

import android.content.Context
import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.CastManager
import dev.jdtech.jellyfin.MainActivity
import dev.jdtech.jellyfin.api.JellyfinApi
import timber.log.Timber
import javax.inject.Inject

class ChromecastManager
@Inject
constructor(
    @ApplicationContext
    private val context: Context,
    private val sessionManagerListener: ChromecastSessionListener
) : CastManager {
    private var castSession: CastSession? = null
    private lateinit var sessionManager: SessionManager

    override fun init(mainActivity: MainActivity) {
        sessionManager = CastContext.getSharedInstance(mainActivity).sessionManager
    }

    override fun onResume() {
        castSession = sessionManager.currentCastSession
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    override fun onStop() {
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    override fun addCastMenuItem(menu: Menu, menuItemId: Int) {
        CastButtonFactory.setUpMediaRouteButton(
            context,
            menu,
            menuItemId
        )
    }
}

class ChromecastSessionListener
@Inject
constructor(private val jellyfinApi: JellyfinApi) : SessionManagerListener<CastSession> {
    private var castSession: CastSession? = null

    override fun onSessionEnded(session: CastSession, p1: Int) {
        castSession = null
    }

    override fun onSessionEnding(session: CastSession) {
    }

    override fun onSessionResumeFailed(session: CastSession, p1: Int) {
    }

    override fun onSessionResumed(session: CastSession, p1: Boolean) {
    }

    override fun onSessionResuming(session: CastSession, p1: String) {
    }

    override fun onSessionStartFailed(session: CastSession, p1: Int) {
    }

    override fun onSessionStarted(session: CastSession, p1: String) {
//        mainActivity.invalidateOptionsMenu()

        val castMessage = """
            {
                "options":{},
                "command": "Identify",
                "userId": "${jellyfinApi.userId}",
                "deviceId": "${jellyfinApi.api.deviceInfo.id}",
                "accessToken": "${jellyfinApi.api.accessToken}",
                "serverAddress": "${jellyfinApi.api.baseUrl}",
                "serverId": "",
                "serverVersion": "",
                "receiverName": ""
            }
        """.trimIndent()

        session.sendMessage("urn:x-cast:com.connectsdk", castMessage)
        session.setMessageReceivedCallbacks(
            "urn:x-cast:com.connectsdk"
        ) { _, _, message -> Timber.i(message) }
    }

    override fun onSessionStarting(session: CastSession) {
        castSession = session
    }

    override fun onSessionSuspended(session: CastSession, p1: Int) {
    }

}