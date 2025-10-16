package dev.jdtech.jellyfin.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Provides Cast options for the Jellyfin app
 * This uses the Default Media Receiver app ID for basic casting support
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        // Using the Default Media Receiver (basic casting)
        // For custom receiver, replace with your own App ID from Google Cast Console
        val receiverApplicationId = "CC1AD845" // Default Media Receiver
        
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(
                "dev.jdtech.jellyfin.MainActivity"
            )
            .build()
        
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(
                "dev.jdtech.jellyfin.cast.ExpandedControlsActivity"
            )
            .build()
        
        return CastOptions.Builder()
            .setReceiverApplicationId(receiverApplicationId)
            .setCastMediaOptions(mediaOptions)
            .build()
    }
    
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
