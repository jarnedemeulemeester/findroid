package dev.jdtech.jellyfin.dlna

import android.content.Context
import android.widget.Toast
import org.jupnp.controlpoint.ActionCallback
import org.jupnp.model.action.ActionInvocation
import org.jupnp.model.message.UpnpResponse
import org.jupnp.model.meta.Device
import org.jupnp.model.types.UDAServiceType
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.support.avtransport.callback.GetPositionInfo
import org.jupnp.support.avtransport.callback.Pause
import org.jupnp.support.avtransport.callback.Play
import org.jupnp.support.avtransport.callback.Seek
import org.jupnp.support.avtransport.callback.SetAVTransportURI
import org.jupnp.support.avtransport.callback.Stop
import org.jupnp.support.model.SeekMode
import org.jupnp.support.contentdirectory.DIDLParser
import org.jupnp.support.model.DIDLContent
import org.jupnp.support.model.PositionInfo
import org.jupnp.support.model.ProtocolInfo
import org.jupnp.support.model.Res
import org.jupnp.support.model.item.VideoItem
import org.jupnp.support.renderingcontrol.callback.GetVolume
import org.jupnp.support.renderingcontrol.callback.SetVolume
import timber.log.Timber
import android.os.Handler
import android.os.Looper

/**
 * Helper class to manage DLNA/UPnP functionality
 */
object DlnaHelper {
    
    private var currentDevice: Device<*, *, *>? = null
    private var currentMediaTitle: String = ""
    private var currentMediaSubtitle: String = ""
    private var currentMediaImageUrl: String? = null
    private var currentDuration: Long = 0L
    private var currentPosition: Long = 0L
    private var isPlaying: Boolean = false
    private var seekSupported: Boolean = true // Assume supported until proven otherwise
    
    private val INSTANCE_ID = UnsignedIntegerFourBytes(0L)
    
    /**
     * Check if a DLNA device is currently selected
     */
    fun isDlnaDeviceAvailable(context: Context): Boolean {
        return currentDevice != null && DlnaDeviceManager.isInitialized()
    }
    
    /**
     * Get the currently selected DLNA device
     */
    fun getCurrentDevice(): Device<*, *, *>? {
        return currentDevice
    }
    
    /**
     * Set the current DLNA device
     */
    fun setCurrentDevice(device: Device<*, *, *>?) {
        currentDevice = device
        seekSupported = true // Reset seek support flag for new device
        Timber.d("Current DLNA device set: ${device?.displayString}")
    }
    
    /**
     * Load media to DLNA device
     */
    fun loadMedia(
        context: Context,
        contentUrl: String,
        contentType: String,
        title: String,
        subtitle: String? = null,
        imageUrl: String? = null,
        position: Long = 0,
        duration: Long = 0
    ): Boolean {
        try {
            val device = currentDevice ?: run {
                Timber.e("No DLNA device selected")
                return false
            }
            
            // Store metadata
            currentMediaTitle = title
            currentMediaSubtitle = subtitle ?: ""
            currentMediaImageUrl = imageUrl
            currentDuration = duration
            currentPosition = position
            
            // Get AVTransport service
            val avTransportService = device.findService(UDAServiceType("AVTransport")) ?: run {
                Timber.e("AVTransport service not found on device")
                return false
            }
            
            Timber.d("=== Loading media to DLNA device ===")
            Timber.d("  URL: $contentUrl")
            Timber.d("  Type: $contentType")
            Timber.d("  Title: $title")
            
            // Create DIDL content
            val res = Res().apply {
                value = contentUrl
                protocolInfo = ProtocolInfo("http-get:*:$contentType:*")
                if (duration > 0) {
                    this.duration = formatDuration(duration)
                }
            }
            
            val videoItem = VideoItem(
                "0",
                "0",
                title,
                "",
                res
            )
            
            val content = DIDLContent().apply {
                addItem(videoItem)
            }
            
            // Convert DIDL content to XML string
            val didlXml = DIDLParser().generate(content)
            
            // Get UPnP service to execute actions
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                Timber.e("UPnP service not available")
                return false
            }
            
            // Set the media URI
            val setAVTransportCallback = object : SetAVTransportURI(
                avTransportService,
                contentUrl,
                didlXml
            ) {
                override fun success(invocation: ActionInvocation<*>) {
                    Timber.d("✓ SetAVTransportURI successful")
                    
                    // Wait a bit before sending Play command to let the device process the URI
                    // Some devices need time to load and prepare the media
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Auto-play after setting URI
                        val playCallback = object : Play(INSTANCE_ID, avTransportService) {
                            override fun success(invocation: ActionInvocation<*>) {
                                Timber.d("✓ Play command successful")
                                isPlaying = true
                            }
                            
                            override fun failure(
                                invocation: ActionInvocation<*>,
                                operation: UpnpResponse,
                                defaultMsg: String
                            ) {
                                Timber.e("✗ Play command failed: $defaultMsg")
                                Timber.e("  Response code: ${operation.statusCode}")
                                Timber.e("  Response message: ${operation.responseDetails}")
                            }
                        }
                        
                        upnpService.controlPoint?.execute(playCallback)
                    }, 2000) // Wait 2 seconds before playing
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    Timber.e("✗ SetAVTransportURI failed: $defaultMsg")
                }
            }
            
            upnpService.controlPoint?.execute(setAVTransportCallback)
            
            return true
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading media to DLNA")
            return false
        }
    }
    
    /**
     * Stop DLNA playback and clear device selection
     */
    fun stopDlna(context: Context) {
        try {
            val device = currentDevice
            if (device != null) {
                val avTransportService = device.findService(UDAServiceType("AVTransport"))
                if (avTransportService != null) {
                    val upnpService = DlnaDeviceManager.getUpnpService()
                    if (upnpService != null) {
                        val stopCallback = object : Stop(INSTANCE_ID, avTransportService) {
                            override fun success(invocation: ActionInvocation<*>) {
                                Timber.d("✓ Stop command successful")
                            }
                            
                            override fun failure(
                                invocation: ActionInvocation<*>,
                                operation: UpnpResponse,
                                defaultMsg: String
                            ) {
                                Timber.e("✗ Stop command failed: $defaultMsg")
                            }
                        }
                        upnpService.controlPoint?.execute(stopCallback)
                    }
                }
            }
            
            currentDevice = null
            isPlaying = false
            Timber.d("DLNA stopped and device cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping DLNA")
        }
    }
    
    /**
     * Play/Resume media on DLNA device
     */
    fun play(context: Context) {
        try {
            val device = currentDevice ?: run {
                Timber.w("No DLNA device selected")
                return
            }
            
            val avTransportService = device.findService(UDAServiceType("AVTransport")) ?: run {
                Timber.e("AVTransport service not found")
                return
            }
            
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                Timber.e("UPnP service not available")
                return
            }
            
            val playCallback = object : Play(INSTANCE_ID, avTransportService) {
                override fun success(invocation: ActionInvocation<*>) {
                    Timber.d("✓ Play command successful")
                    isPlaying = true
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    Timber.e("✗ Play command failed: $defaultMsg")
                }
            }
            
            upnpService.controlPoint?.execute(playCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error playing on DLNA")
        }
    }
    
    /**
     * Pause media on DLNA device
     */
    fun pause(context: Context) {
        try {
            val device = currentDevice ?: run {
                Timber.w("No DLNA device selected")
                return
            }
            
            val avTransportService = device.findService(UDAServiceType("AVTransport")) ?: run {
                Timber.e("AVTransport service not found")
                return
            }
            
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                Timber.e("UPnP service not available")
                return
            }
            
            val pauseCallback = object : Pause(INSTANCE_ID, avTransportService) {
                override fun success(invocation: ActionInvocation<*>) {
                    Timber.d("✓ Pause command successful")
                    isPlaying = false
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    Timber.e("✗ Pause command failed: $defaultMsg")
                }
            }
            
            upnpService.controlPoint?.execute(pauseCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error pausing on DLNA")
        }
    }
    
    /**
     * Seek to position on DLNA device
     * @param positionMs Position in milliseconds
     */
    fun seek(context: Context, positionMs: Long) {
        // Skip seek if we know the device doesn't support it
        if (!seekSupported) {
            Timber.d("Seek skipped - device doesn't support it")
            currentPosition = positionMs // Just update our local position
            return
        }
        
        try {
            val device = currentDevice ?: run {
                Timber.w("No DLNA device selected")
                return
            }
            
            val avTransportService = device.findService(UDAServiceType("AVTransport")) ?: run {
                Timber.e("AVTransport service not found")
                return
            }
            
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                Timber.e("UPnP service not available")
                return
            }
            
            val target = formatDuration(positionMs)
            
            // Use REL_TIME seek mode which is more widely supported
            val seekCallback = object : Seek(avTransportService, SeekMode.REL_TIME, target) {
                override fun success(invocation: ActionInvocation<*>) {
                    Timber.d("✓ Seek command successful to $target")
                    currentPosition = positionMs
                    seekSupported = true
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    Timber.w("Seek not supported on this device: $defaultMsg")
                    
                    // Show toast only on first failure
                    if (seekSupported) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Este dispositivo no es compatible con avanzar/retroceder",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    
                    seekSupported = false // Mark as unsupported for future calls
                    currentPosition = positionMs // Update position locally
                }
            }
            
            upnpService.controlPoint?.execute(seekCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error seeking on DLNA")
            seekSupported = false
        }
    }
    
    /**
     * Get current playback position in milliseconds
     */
    fun getCurrentPosition(context: Context, callback: (Long) -> Unit) {
        try {
            val device = currentDevice ?: run {
                callback(0L)
                return
            }
            
            val avTransportService = device.findService(UDAServiceType("AVTransport")) ?: run {
                callback(currentPosition)
                return
            }
            
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                callback(currentPosition)
                return
            }
            
            val getPositionCallback = object : GetPositionInfo(INSTANCE_ID, avTransportService) {
                override fun received(invocation: ActionInvocation<*>, positionInfo: PositionInfo) {
                    try {
                        // Update playing state based on transport state from action output
                        val transportStateValue = invocation.getOutput("TransportState")?.value?.toString()
                        isPlaying = transportStateValue == "PLAYING"
                        
                        val relTime = positionInfo.relTime
                        if (relTime != null && relTime != "NOT_IMPLEMENTED") {
                            val parts = relTime.split(":")
                            if (parts.size == 3) {
                                val hours = parts[0].toLong()
                                val minutes = parts[1].toLong()
                                val seconds = parts[2].toLong()
                                val positionMs = (hours * 3600 + minutes * 60 + seconds) * 1000
                                currentPosition = positionMs
                                callback(positionMs)
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing position")
                    }
                    callback(currentPosition)
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    callback(currentPosition)
                }
            }
            
            upnpService.controlPoint?.execute(getPositionCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error getting position")
            callback(0L)
        }
    }
    
    /**
     * Get media duration in milliseconds
     */
    fun getDuration(context: Context): Long {
        return currentDuration
    }
    
    /**
     * Set volume level (0.0 to 1.0)
     */
    fun setVolume(context: Context, volume: Double) {
        try {
            val device = currentDevice ?: run {
                Timber.w("No DLNA device selected")
                return
            }
            
            val renderingControlService = device.findService(UDAServiceType("RenderingControl")) ?: run {
                Timber.w("RenderingControl service not found")
                return
            }
            
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                Timber.e("UPnP service not available")
                return
            }
            
            val volumeInt = (volume * 100).toInt().coerceIn(0, 100)
            
            val setVolumeCallback = object : SetVolume(INSTANCE_ID, renderingControlService, volumeInt.toLong()) {
                override fun success(invocation: ActionInvocation<*>) {
                    Timber.d("✓ Volume set to $volumeInt")
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    Timber.e("✗ Set volume failed: $defaultMsg")
                }
            }
            
            upnpService.controlPoint?.execute(setVolumeCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error setting volume")
        }
    }
    
    /**
     * Get current volume level (0.0 to 1.0)
     */
    fun getVolume(context: Context, callback: (Double) -> Unit) {
        try {
            val device = currentDevice ?: run {
                callback(0.5)
                return
            }
            
            val renderingControlService = device.findService(UDAServiceType("RenderingControl")) ?: run {
                callback(0.5)
                return
            }
            
            val upnpService = DlnaDeviceManager.getUpnpService() ?: run {
                callback(0.5)
                return
            }
            
            val getVolumeCallback = object : GetVolume(INSTANCE_ID, renderingControlService) {
                override fun received(invocation: ActionInvocation<*>, currentVolume: Int) {
                    val volumeDouble = currentVolume / 100.0
                    callback(volumeDouble)
                }
                
                override fun failure(
                    invocation: ActionInvocation<*>,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    callback(0.5)
                }
            }
            
            upnpService.controlPoint?.execute(getVolumeCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error getting volume")
            callback(0.5)
        }
    }
    
    /**
     * Get media title
     */
    fun getMediaTitle(context: Context): String {
        return currentMediaTitle
    }
    
    /**
     * Get media subtitle
     */
    fun getMediaSubtitle(context: Context): String {
        return currentMediaSubtitle
    }
    
    /**
     * Get media image URL
     */
    fun getMediaImageUrl(context: Context): String? {
        return currentMediaImageUrl
    }
    
    /**
     * Check if media is currently playing
     */
    fun isPlaying(): Boolean {
        return isPlaying
    }
    
    /**
     * Format duration in milliseconds to HH:MM:SS format
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
