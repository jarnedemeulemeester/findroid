package dev.jdtech.jellyfin.dlna

import android.content.Context
import org.jupnp.model.meta.Device
import org.jupnp.model.meta.LocalDevice
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.registry.DefaultRegistryListener
import org.jupnp.registry.Registry
import timber.log.Timber

/**
 * Singleton manager for DLNA device discovery and management
 */
object DlnaDeviceManager {
    
    private val discoveredDevices = mutableListOf<Device<*, *, *>>()
    private val deviceListeners = mutableListOf<DeviceListener>()
    private var isInitialized = false
    private var simpleUpnpService: SimpleUpnpService? = null
    
    /**
     * Interface for listening to device discovery events
     */
    interface DeviceListener {
        fun onDeviceAdded(device: Device<*, *, *>)
        fun onDeviceRemoved(device: Device<*, *, *>)
    }
    
    private val registryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            Timber.d("=== REMOTE DEVICE ADDED ===")
            val deviceInfo = buildString {
                append("Device: ${device.displayString}")
                append("\n  Type: ${device.type}")
                append("\n  Manufacturer: ${device.details.manufacturerDetails?.manufacturer}")
                append("\n  Model: ${device.details.modelDetails?.modelName}")
                append("\n  Services: ${device.services.joinToString { it.serviceType.type }}")
            }
            Timber.d(deviceInfo)
            
            // TEMPORARILY: Add ALL devices to see what's being discovered
            // Check if device has media renderer capabilities
            // if (isMediaRenderer(device)) {
                addDevice(device)
                Timber.d("Device added to list")
            // } else {
            //     Timber.d("Device ${device.displayString} does not have media renderer capabilities, skipping")
            // }
        }
        
        override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
            Timber.d("=== REMOTE DEVICE REMOVED ===")
            Timber.d("Device: ${device.displayString}")
            removeDevice(device)
        }
        
        override fun localDeviceAdded(registry: Registry, device: LocalDevice) {
            Timber.d("=== LOCAL DEVICE ADDED ===")
            Timber.d("Device: ${device.displayString}")
            // if (isMediaRenderer(device)) {
                addDevice(device)
            // }
        }
        
        override fun localDeviceRemoved(registry: Registry, device: LocalDevice) {
            Timber.d("=== LOCAL DEVICE REMOVED ===")
            Timber.d("Device: ${device.displayString}")
            removeDevice(device)
        }
    }
    
    /**
     * Check if a device is a media renderer (TV, receiver, etc.)
     */
    private fun isMediaRenderer(device: Device<*, *, *>): Boolean {
        // Check device type - looking for MediaRenderer
        val isRenderer = device.type?.type?.contains("MediaRenderer", ignoreCase = true) == true
        
        // Check if device has AVTransport service (needed for playback)
        val hasAVTransport = device.services.any { 
            it.serviceType.type.equals("AVTransport", ignoreCase = true) 
        }
        
        // Check if device has RenderingControl service (needed for volume, etc.)
        val hasRenderingControl = device.services.any { 
            it.serviceType.type.equals("RenderingControl", ignoreCase = true) 
        }
        
        // LG TVs and many Smart TVs expose at least AVTransport
        val hasMediaCapability = hasAVTransport || hasRenderingControl
        
        Timber.d("Device ${device.displayString}: isRenderer=$isRenderer, hasAVTransport=$hasAVTransport, hasRenderingControl=$hasRenderingControl")
        
        // Accept device if it's a renderer OR has media capabilities
        return isRenderer || hasMediaCapability
    }
    
    /**
     * Initialize the DLNA device manager
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Timber.d("DlnaDeviceManager already initialized")
            return
        }
        
        Timber.d("=== Initializing DLNA Device Manager ===")
        
        try {
            // Initialize SimpleUpnpService
            simpleUpnpService = SimpleUpnpService.getInstance(context.applicationContext)
            
            // Add registry listener
            simpleUpnpService?.getService()?.registry?.addListener(registryListener)
            Timber.d("✓ Registry listener added")
            
            // Start initial device search
            simpleUpnpService?.getService()?.controlPoint?.search()
            Timber.d("✓ Initial device search started")
            
            isInitialized = true
            Timber.d("✓ DLNA Device Manager initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "✗ Failed to initialize DLNA manager")
        }
    }
    
    /**
     * Check if the manager is initialized
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * Add a device listener
     */
    fun addDeviceListener(listener: DeviceListener) {
        if (!deviceListeners.contains(listener)) {
            deviceListeners.add(listener)
        }
    }
    
    /**
     * Remove a device listener
     */
    fun removeDeviceListener(listener: DeviceListener) {
        deviceListeners.remove(listener)
    }
    
    /**
     * Get list of currently discovered devices
     */
    fun getDiscoveredDevices(): List<Device<*, *, *>> {
        return discoveredDevices.toList()
    }
    
    /**
     * Refresh device list
     */
    fun refreshDevices() {
        Timber.d("=== REFRESHING DLNA DEVICES ===")
        
        if (simpleUpnpService == null) {
            Timber.w("SimpleUpnpService is null, cannot search for devices")
            return
        }
        
        val service = simpleUpnpService?.getService()
        if (service?.controlPoint == null) {
            Timber.w("ControlPoint is null - service not ready")
            return
        }
        
        try {
            Timber.d("✓ ControlPoint is ready, starting searches...")
            service.controlPoint?.search()
            service.controlPoint?.search(org.jupnp.model.message.header.STAllHeader())
            service.controlPoint?.search(
                org.jupnp.model.message.header.DeviceTypeHeader(
                    org.jupnp.model.types.UDADeviceType("MediaRenderer", 1)
                )
            )
            Timber.d("✓ DLNA device search completed")
        } catch (e: Exception) {
            Timber.e(e, "Error during device search")
        }
    }
    
    /**
     * Get the UPnP service instance
     */
    fun getUpnpService(): org.jupnp.UpnpService? {
        return simpleUpnpService?.getService()
    }
    
    /**
     * Manually add a device (for testing or manual configuration)
     */
    private fun addDevice(device: Device<*, *, *>) {
        if (!discoveredDevices.any { it.identity.udn == device.identity.udn }) {
            discoveredDevices.add(device)
            deviceListeners.forEach { it.onDeviceAdded(device) }
            Timber.d("Device added: ${device.displayString}")
        }
    }
    
    /**
     * Remove a device
     */
    private fun removeDevice(device: Device<*, *, *>) {
        if (discoveredDevices.removeAll { it.identity.udn == device.identity.udn }) {
            deviceListeners.forEach { it.onDeviceRemoved(device) }
            Timber.d("Device removed: ${device.displayString}")
        }
    }
    
    /**
     * Clear all discovered devices
     */
    fun clearDevices() {
        discoveredDevices.clear()
        Timber.d("All devices cleared")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            simpleUpnpService?.getService()?.registry?.removeListener(registryListener)
            simpleUpnpService?.shutdown()
            Timber.d("✓ SimpleUpnpService shutdown")
        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
        }
        
        simpleUpnpService = null
        clearDevices()
        deviceListeners.clear()
        isInitialized = false
        Timber.d("DLNA Device Manager cleaned up")
    }
}
