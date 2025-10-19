package dev.jdtech.jellyfin.roku

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document

data class RokuDevice(
    val name: String,
    val ipAddress: String,
    val port: Int = 8060,
    val modelName: String = "",
    val serialNumber: String = ""
)

object RokuHelper {
    
    private var currentDevice: RokuDevice? = null
    private const val SSDP_PORT = 1900
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_SEARCH_MSG = 
        "M-SEARCH * HTTP/1.1\r\n" +
        "HOST: 239.255.255.250:1900\r\n" +
        "MAN: \"ssdp:discover\"\r\n" +
        "MX: 3\r\n" +
        "ST: roku:ecp\r\n\r\n"
    
    /**
     * Discover Roku devices on the local network using SSDP
     */
    suspend fun discoverRokuDevices(context: Context): List<RokuDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<RokuDevice>()
        
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000
            
            // Send SSDP discovery message
            val searchMessage = SSDP_SEARCH_MSG.toByteArray()
            val searchPacket = DatagramPacket(
                searchMessage,
                searchMessage.size,
                InetAddress.getByName(SSDP_ADDRESS),
                SSDP_PORT
            )
            
            socket.send(searchPacket)
            Timber.d("Sent Roku discovery message")
            
            // Listen for responses
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < 3000) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    
                    val response = String(packet.data, 0, packet.length)
                    if (response.contains("Roku", ignoreCase = true)) {
                        // Extract location URL
                        val locationRegex = "LOCATION: (.+)".toRegex()
                        val match = locationRegex.find(response)
                        val location = match?.groupValues?.get(1)?.trim()
                        
                        if (location != null) {
                            val device = parseRokuDeviceInfo(location)
                            if (device != null && !devices.any { it.ipAddress == device.ipAddress }) {
                                devices.add(device)
                                Timber.d("Found Roku device: ${device.name} at ${device.ipAddress}")
                            }
                        }
                    }
                } catch (e: IOException) {
                    // Timeout or no more packets
                    break
                }
            }
            
            socket.close()
        } catch (e: Exception) {
            Timber.e(e, "Error discovering Roku devices")
        }
        
        devices
    }
    
    /**
     * Parse Roku device info from device description XML
     */
    private suspend fun parseRokuDeviceInfo(location: String): RokuDevice? = withContext(Dispatchers.IO) {
        try {
            val url = URL(location)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            
            val inputStream = connection.inputStream
            val doc: Document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(inputStream)
            
            val friendlyName = doc.getElementsByTagName("friendlyName")
                .item(0)?.textContent ?: "Roku Device"
            val modelName = doc.getElementsByTagName("modelName")
                .item(0)?.textContent ?: ""
            val serialNumber = doc.getElementsByTagName("serialNumber")
                .item(0)?.textContent ?: ""
            
            val ipAddress = url.host
            
            connection.disconnect()
            
            RokuDevice(
                name = friendlyName,
                ipAddress = ipAddress,
                modelName = modelName,
                serialNumber = serialNumber
            )
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Roku device info from $location")
            null
        }
    }
    
    /**
     * Set the current Roku device to use
     */
    fun setCurrentDevice(device: RokuDevice?) {
        currentDevice = device
        Timber.d("Current Roku device set to: ${device?.name ?: "none"}")
    }
    
    /**
     * Get the current Roku device
     */
    fun getCurrentDevice(): RokuDevice? = currentDevice
    
    /**
     * Check if a Roku device is connected
     */
    fun isRokuDeviceAvailable(): Boolean {
        return currentDevice != null
    }
    
    /**
     * Launch Jellyfin app on Roku and play media
     */
    suspend fun playMedia(
        contentUrl: String,
        title: String,
        subtitle: String = "",
        imageUrl: String = "",
        position: Long = 0
    ): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        
        try {
            // First, try to launch Jellyfin channel on Roku
            // Note: This requires the Jellyfin Roku app to be installed
            val launchUrl = "http://${device.ipAddress}:${device.port}/launch/592369" // Jellyfin channel ID
            
            // Build deep link parameters
            val params = buildString {
                append("?contentId=").append(java.net.URLEncoder.encode(contentUrl, "UTF-8"))
                append("&mediaType=video")
                if (title.isNotEmpty()) {
                    append("&title=").append(java.net.URLEncoder.encode(title, "UTF-8"))
                }
                if (position > 0) {
                    append("&position=").append(position / 1000) // Convert to seconds
                }
            }
            
            val url = URL(launchUrl + params)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode == 200) {
                Timber.d("Successfully launched media on Roku")
                return@withContext true
            } else {
                Timber.e("Failed to launch media on Roku, response code: $responseCode")
                return@withContext false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing media on Roku")
            return@withContext false
        }
    }
    
    /**
     * Send keypress command to Roku
     */
    suspend fun sendKeyPress(key: String): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        
        try {
            val url = URL("http://${device.ipAddress}:${device.port}/keypress/$key")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 2000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            Timber.e(e, "Error sending keypress to Roku")
            false
        }
    }
    
    /**
     * Stop playback on Roku
     */
    suspend fun stopPlayback(): Boolean {
        return sendKeyPress("Home")
    }
    
    /**
     * Pause/Resume playback on Roku
     */
    suspend fun pauseResume(): Boolean {
        return sendKeyPress("Play")
    }
    
    /**
     * Clear current Roku device
     */
    fun disconnect() {
        currentDevice = null
        Timber.d("Roku device disconnected")
    }
}
