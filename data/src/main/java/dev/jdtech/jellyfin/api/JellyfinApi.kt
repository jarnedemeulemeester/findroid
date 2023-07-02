package dev.jdtech.jellyfin.api

import android.content.Context
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.data.BuildConfig
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.extensions.devicesApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import java.util.UUID

/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-platform-android
 *
 * @param androidContext The context
 * @param socketTimeout The socket timeout
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(
    androidContext: Context,
    requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
    connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
    socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
) {
    val jellyfin = createJellyfin {
        clientInfo =
            ClientInfo(name = androidContext.applicationInfo.loadLabel(androidContext.packageManager).toString(), version = BuildConfig.VERSION_NAME)
        context = androidContext
    }
    val api = jellyfin.createApi(
        httpClientOptions = HttpClientOptions(
            requestTimeout = requestTimeout,
            connectTimeout = connectTimeout,
            socketTimeout = socketTimeout,
        ),
    )
    var userId: UUID? = null

    val devicesApi = api.devicesApi
    val systemApi = api.systemApi
    val userApi = api.userApi
    val viewsApi = api.userViewsApi
    val itemsApi = api.itemsApi
    val userLibraryApi = api.userLibraryApi
    val showsApi = api.tvShowsApi
    val sessionApi = api.sessionApi
    val videosApi = api.videosApi
    val mediaInfoApi = api.mediaInfoApi
    val playStateApi = api.playStateApi
    val quickConnectApi = api.quickConnectApi

    companion object {
        @Volatile
        private var INSTANCE: JellyfinApi? = null

        fun getInstance(
            context: Context,
            requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
            connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
            socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
        ): JellyfinApi {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = JellyfinApi(
                        androidContext = context.applicationContext,
                        requestTimeout = requestTimeout,
                        connectTimeout = connectTimeout,
                        socketTimeout = socketTimeout,
                    )
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
