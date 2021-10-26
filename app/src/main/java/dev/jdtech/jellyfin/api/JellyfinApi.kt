package dev.jdtech.jellyfin.api

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import dev.jdtech.jellyfin.BuildConfig
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.JellyfinOptions
import org.jellyfin.sdk.api.client.KtorClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.util.ApiClientFactory
import java.util.UUID

/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-platform-android
 *
 * @param androidContext The context
 * @param baseUrl The url of the server
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(androidContext: Context, baseUrl: String) {

    val jellyfin = Jellyfin(
        JellyfinOptions(
            context = androidContext,
            clientInfo = ClientInfo(
                name = androidContext.applicationInfo.loadLabel(androidContext.packageManager)
                    .toString(),
                version = BuildConfig.VERSION_NAME
            ),
            deviceInfo = DeviceInfo(UUID.randomUUID().toString(), deviceName(androidContext)),
            apiClientFactory = ApiClientFactory(::KtorClient)
        )
    )

    val api = jellyfin.createApi(baseUrl = baseUrl)
    var userId: UUID? = null

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

    private fun deviceName(context: Context) = PreferenceManager
        .getDefaultSharedPreferences(context)
        .getString("device_name", null) ?: Build.MODEL

    companion object {
        @Volatile
        private var INSTANCE: JellyfinApi? = null

        /**
         * Creates or gets a new instance of [JellyfinApi]
         *
         * If there already is an instance, it will return that instance and ignore the [baseUrl] parameter
         *
         * @param context The context
         * @param baseUrl The url of the server
         */
        fun getInstance(context: Context, baseUrl: String): JellyfinApi {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = JellyfinApi(context.applicationContext, baseUrl)
                    INSTANCE = instance
                }
                return instance
            }
        }

        /**
         * Create a new [JellyfinApi] instance
         *
         * @param context The context
         * @param baseUrl The url of the server
         */
        fun newInstance(context: Context, baseUrl: String): JellyfinApi {
            synchronized(this) {
                val instance = JellyfinApi(context.applicationContext, baseUrl)
                INSTANCE = instance
                return instance
            }
        }
    }
}