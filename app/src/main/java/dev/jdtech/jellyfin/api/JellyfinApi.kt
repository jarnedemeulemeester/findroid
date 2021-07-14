package dev.jdtech.jellyfin.api

import android.content.Context
import dev.jdtech.jellyfin.BuildConfig
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.android
import org.jellyfin.sdk.api.operations.*
import org.jellyfin.sdk.model.ClientInfo
import java.util.*


/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-platform-android
 *
 * @param context The context
 * @param baseUrl The url of the server
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(context: Context, baseUrl: String) {
    val jellyfin = Jellyfin {
        clientInfo =
            ClientInfo(name = BuildConfig.APPLICATION_ID, version = BuildConfig.VERSION_NAME)
        android(context)
    }
    val api = jellyfin.createApi(baseUrl = baseUrl)
    var userId: UUID? = null

    val systemApi = SystemApi(api)
    val userApi = UserApi(api)
    val viewsApi = UserViewsApi(api)
    val itemsApi = ItemsApi(api)
    val userLibraryApi = UserLibraryApi(api)
    val showsApi = TvShowsApi(api)
    val sessionApi = SessionApi(api)
    val videosApi = VideosApi(api)
    val mediaInfoApi = MediaInfoApi(api)
    val playstateApi = PlayStateApi(api)

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