package dev.jdtech.jellyfin.api

import android.content.Context
import dev.jdtech.jellyfin.data.BuildConfig
import dev.jdtech.jellyfin.settings.domain.Constants
import okhttp3.OkHttpClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.devicesApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.mediaSegmentsApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.suggestionsApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.trickplayApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-platform-android
 *
 * @param androidContext The context
 * @param okHttpClient Optional custom OkHttpClient for proxy support
 * @param requestTimeout The request timeout
 * @param connectTimeout The connect timeout
 * @param socketTimeout The socket timeout
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(
    androidContext: Context,
    okHttpClient: OkHttpClient? = null,
    requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
    connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
    socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
) {
    // Create OkHttpFactory with custom client for proxy support
    private val okHttpFactory = okHttpClient?.let { OkHttpFactory(it) } ?: OkHttpFactory()

    val jellyfin = createJellyfin {
        clientInfo =
            ClientInfo(name = androidContext.applicationInfo.loadLabel(androidContext.packageManager).toString(), version = BuildConfig.VERSION_NAME)
        context = androidContext
        apiClientFactory = okHttpFactory
        socketConnectionFactory = okHttpFactory
    }
    val api = jellyfin.createApi(
        httpClientOptions = HttpClientOptions(
            requestTimeout = requestTimeout.toDuration(DurationUnit.MILLISECONDS),
            connectTimeout = connectTimeout.toDuration(DurationUnit.MILLISECONDS),
            socketTimeout = socketTimeout.toDuration(DurationUnit.MILLISECONDS),
        ),
    )
    var userId: UUID? = null

    val brandingApi = api.brandingApi
    val devicesApi = api.devicesApi
    val itemsApi = api.itemsApi
    val mediaInfoApi = api.mediaInfoApi
    val mediaSegmentsApi = api.mediaSegmentsApi
    val playStateApi = api.playStateApi
    val quickConnectApi = api.quickConnectApi
    val sessionApi = api.sessionApi
    val showsApi = api.tvShowsApi
    val suggestionsApi = api.suggestionsApi
    val systemApi = api.systemApi
    val trickplayApi = api.trickplayApi
    val userApi = api.userApi
    val userLibraryApi = api.userLibraryApi
    val videosApi = api.videosApi
    val viewsApi = api.userViewsApi

    companion object {
        @Volatile
        private var INSTANCE: JellyfinApi? = null

        fun getInstance(
            context: Context,
            okHttpClient: OkHttpClient? = null,
            requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
            connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
            socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
        ): JellyfinApi {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = JellyfinApi(
                        androidContext = context.applicationContext,
                        okHttpClient = okHttpClient,
                        requestTimeout = requestTimeout,
                        connectTimeout = connectTimeout,
                        socketTimeout = socketTimeout,
                    )
                    INSTANCE = instance
                }
                return instance
            }
        }

        /**
         * Clears the singleton instance. Call this when proxy settings change
         * to force recreation of the API client with new settings.
         */
        fun clearInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}
