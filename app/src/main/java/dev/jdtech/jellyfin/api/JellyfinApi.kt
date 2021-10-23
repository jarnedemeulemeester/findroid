package dev.jdtech.jellyfin.api

import android.content.Context
import dev.jdtech.jellyfin.BuildConfig
import org.jellyfin.sdk.api.client.extensions.*
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import java.util.*


/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-platform-android
 *
 * @param androidContext The context
 * @param baseUrl The url of the server
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(androidContext: Context, baseUrl: String) {
    val jellyfin = createJellyfin {
        clientInfo =
            ClientInfo(name = androidContext.applicationInfo.loadLabel(androidContext.packageManager).toString(), version = BuildConfig.VERSION_NAME)
        context = androidContext
    }
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