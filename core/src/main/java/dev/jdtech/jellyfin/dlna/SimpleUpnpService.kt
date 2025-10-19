package dev.jdtech.jellyfin.dlna

import android.content.Context
import android.net.wifi.WifiManager
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceImpl
import org.jupnp.android.AndroidUpnpServiceConfiguration
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Simple UPnP service implementation that doesn't rely on Android Service
 * This creates the UPnP service directly in the application context
 */
class SimpleUpnpService private constructor(context: Context) {
    
    private var upnpService: UpnpService? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    
    init {
        try {
            Timber.d("=== Initializing SimpleUpnpService ===")
            
            // Acquire multicast lock
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("jellycast_upnp")?.apply {
                setReferenceCounted(true)
                acquire()
                Timber.d("✓ Multicast lock acquired")
            }
            
            // Create UPnP service configuration
            val configuration = object : AndroidUpnpServiceConfiguration() {
                override fun getRegistryMaintenanceIntervalMillis(): Int {
                    return 3000 // Check every 3 seconds
                }
                
                override fun createDefaultExecutorService() = Executors.newCachedThreadPool()
            }
            
            // Create UPnP service directly
            Timber.d("Creating UpnpServiceImpl...")
            upnpService = UpnpServiceImpl(configuration)
            
            // Start the service
            Timber.d("Starting UPnP service...")
            upnpService?.startup()
            
            Timber.d("✓ UpnpService created and started")
            Timber.d("  - ControlPoint: ${upnpService?.controlPoint}")
            Timber.d("  - Registry: ${upnpService?.registry}")
            
            if (upnpService?.controlPoint != null) {
                Timber.d("=== SimpleUpnpService initialization SUCCESSFUL ===")
            } else {
                Timber.e("=== SimpleUpnpService initialization FAILED - ControlPoint is null ===")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error initializing SimpleUpnpService")
        }
    }
    
    fun getService(): UpnpService? = upnpService
    
    fun shutdown() {
        try {
            Timber.d("Shutting down SimpleUpnpService")
            upnpService?.shutdown()
            multicastLock?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error shutting down SimpleUpnpService")
        }
    }
    
    companion object {
        @Volatile
        private var instance: SimpleUpnpService? = null
        
        fun getInstance(context: Context): SimpleUpnpService {
            return instance ?: synchronized(this) {
                instance ?: SimpleUpnpService(context.applicationContext).also { instance = it }
            }
        }
    }
}
