package dev.jdtech.jellyfin.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.images.WebImage
import timber.log.Timber

/**
 * Helper class to manage Chromecast functionality
 */
object CastHelper {
    
    /**
     * Check if a Cast session is currently connected
     */
    fun isCastSessionAvailable(context: Context): Boolean {
        return try {
            val castContext = CastContext.getSharedInstance(context)
            castContext.sessionManager.currentCastSession?.isConnected == true
        } catch (e: Exception) {
            Timber.e(e, "Error checking cast session")
            false
        }
    }
    
    /**
     * Get the current Cast session
     */
    fun getCastSession(context: Context): CastSession? {
        return try {
            val castContext = CastContext.getSharedInstance(context)
            castContext.sessionManager.currentCastSession
        } catch (e: Exception) {
            Timber.e(e, "Error getting cast session")
            null
        }
    }
    
    /**
     * Load media to Chromecast
     */
    fun loadMedia(
        context: Context,
        contentUrl: String,
        contentType: String,
        title: String,
        subtitle: String? = null,
        imageUrl: String? = null,
        position: Long = 0
    ): Boolean {
        try {
            val castSession = getCastSession(context) ?: return false
            val remoteMediaClient = castSession.remoteMediaClient ?: return false
            
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, title)
                subtitle?.let { putString(MediaMetadata.KEY_SUBTITLE, it) }
                imageUrl?.let { 
                    addImage(WebImage(android.net.Uri.parse(it)))
                }
            }
            
            val mediaInfo = MediaInfo.Builder(contentUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .setMetadata(metadata)
                .build()
            
            val request = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setCurrentTime(position)
                .setAutoplay(true)
                .build()
            
            remoteMediaClient.load(request)
            
            Timber.d("Media loaded to Chromecast: $title")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error loading media to Chromecast")
            return false
        }
    }
    
    /**
     * Stop casting
     */
    fun stopCasting(context: Context) {
        try {
            val castContext = CastContext.getSharedInstance(context)
            castContext.sessionManager.endCurrentSession(true)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping cast")
        }
    }
}
