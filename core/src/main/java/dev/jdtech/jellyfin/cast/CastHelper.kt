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
    
    /**
     * Play/Resume media on Chromecast
     */
    fun play(context: Context) {
        try {
            getCastSession(context)?.remoteMediaClient?.play()
        } catch (e: Exception) {
            Timber.e(e, "Error playing on Chromecast")
        }
    }
    
    /**
     * Pause media on Chromecast
     */
    fun pause(context: Context) {
        try {
            getCastSession(context)?.remoteMediaClient?.pause()
        } catch (e: Exception) {
            Timber.e(e, "Error pausing on Chromecast")
        }
    }
    
    /**
     * Seek to position on Chromecast
     * @param positionMs Position in milliseconds
     */
    fun seek(context: Context, positionMs: Long) {
        try {
            getCastSession(context)?.remoteMediaClient?.seek(positionMs)
        } catch (e: Exception) {
            Timber.e(e, "Error seeking on Chromecast")
        }
    }
    
    /**
     * Get current playback position in milliseconds
     */
    fun getCurrentPosition(context: Context): Long {
        return try {
            getCastSession(context)?.remoteMediaClient?.approximateStreamPosition ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Error getting position")
            0L
        }
    }
    
    /**
     * Get media duration in milliseconds
     */
    fun getDuration(context: Context): Long {
        return try {
            getCastSession(context)?.remoteMediaClient?.streamDuration ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Error getting duration")
            0L
        }
    }
    
    /**
     * Check if media is currently playing
     */
    fun isPlaying(context: Context): Boolean {
        return try {
            getCastSession(context)?.remoteMediaClient?.isPlaying == true
        } catch (e: Exception) {
            Timber.e(e, "Error checking play state")
            false
        }
    }
    
    /**
     * Set volume level (0.0 to 1.0)
     */
    fun setVolume(context: Context, volume: Double) {
        try {
            getCastSession(context)?.let { session ->
                session.setVolume(volume.coerceIn(0.0, 1.0))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting volume")
        }
    }
    
    /**
     * Get current volume level (0.0 to 1.0)
     */
    fun getVolume(context: Context): Double {
        return try {
            getCastSession(context)?.volume ?: 0.5
        } catch (e: Exception) {
            Timber.e(e, "Error getting volume")
            0.5
        }
    }
    
    /**
     * Get media title
     */
    fun getMediaTitle(context: Context): String {
        return try {
            getCastSession(context)?.remoteMediaClient?.mediaInfo?.metadata?.getString(
                com.google.android.gms.cast.MediaMetadata.KEY_TITLE
            ) ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Error getting media title")
            ""
        }
    }
    
    /**
     * Get media subtitle
     */
    fun getMediaSubtitle(context: Context): String {
        return try {
            getCastSession(context)?.remoteMediaClient?.mediaInfo?.metadata?.getString(
                com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE
            ) ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Error getting media subtitle")
            ""
        }
    }
    
    /**
     * Get media image URL
     */
    fun getMediaImageUrl(context: Context): String? {
        return try {
            getCastSession(context)?.remoteMediaClient?.mediaInfo?.metadata?.images?.firstOrNull()?.url?.toString()
        } catch (e: Exception) {
            Timber.e(e, "Error getting media image")
            null
        }
    }
}
