package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.dlna.DlnaDeviceManager
import dev.jdtech.jellyfin.dlna.DlnaHelper
import kotlinx.coroutines.delay
import org.jupnp.model.meta.Device
import timber.log.Timber

/**
 * Dialog to show available DLNA devices and allow selection
 */
@Composable
fun DlnaDevicePicker(
    onDeviceSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val devices = remember { mutableStateListOf<Device<*, *, *>>() }
    var isSearching by remember { mutableStateOf(true) }
    
    val deviceListener = remember {
        object : DlnaDeviceManager.DeviceListener {
            override fun onDeviceAdded(device: Device<*, *, *>) {
                // Only add devices that have AVTransport service (required for DLNA media control)
                val hasAVTransport = device.services.any { 
                    it.serviceType.type == "AVTransport" || 
                    it.serviceType.toString().contains("AVTransport")
                }
                
                if (hasAVTransport && !devices.any { it.identity.udn == device.identity.udn }) {
                    Timber.d("Device has AVTransport service: ${device.details.friendlyName}")
                    devices.add(device)
                } else if (!hasAVTransport) {
                    Timber.w("Device does not have AVTransport service, skipping: ${device.details.friendlyName}")
                }
                isSearching = false
            }
            
            override fun onDeviceRemoved(device: Device<*, *, *>) {
                devices.removeAll { it.identity.udn == device.identity.udn }
            }
        }
    }
    
    DisposableEffect(Unit) {
        // Initialize DLNA manager if not already initialized
        if (!DlnaDeviceManager.isInitialized()) {
            DlnaDeviceManager.initialize(context)
        }
        
        // Load initial devices and filter only those with AVTransport service
        devices.clear()
        val allDevices = DlnaDeviceManager.getDiscoveredDevices()
        val compatibleDevices = allDevices.filter { device ->
            val hasAVTransport = device.services.any { 
                it.serviceType.type == "AVTransport" || 
                it.serviceType.toString().contains("AVTransport")
            }
            if (!hasAVTransport) {
                Timber.w("Filtering out device without AVTransport: ${device.details.friendlyName}")
            }
            hasAVTransport
        }
        devices.addAll(compatibleDevices)
        
        if (devices.isNotEmpty()) {
            isSearching = false
        }
        
        // Add device listener
        DlnaDeviceManager.addDeviceListener(deviceListener)
        
        // Refresh devices
        DlnaDeviceManager.refreshDevices()
        
        onDispose {
            DlnaDeviceManager.removeDeviceListener(deviceListener)
        }
    }
    
    // Repeated search with delays - some devices take time to respond
    LaunchedEffect(Unit) {
        // Wait for service to fully initialize before first search
        delay(1000)
        
        // Check if service is ready
        if (DlnaDeviceManager.getUpnpService() != null) {
            Timber.d("Service ready, starting searches")
            DlnaDeviceManager.refreshDevices()
            
            // Search again after 2 seconds
            delay(2000)
            DlnaDeviceManager.refreshDevices()
            
            // Search again after 4 seconds
            delay(2000)
            DlnaDeviceManager.refreshDevices()
            
            // Final search after 6 seconds
            delay(2000)
            DlnaDeviceManager.refreshDevices()
        } else {
            Timber.w("UPnP service not ready after 1 second")
        }
        
        // Stop showing "searching" indicator after searches complete
        delay(1000)
        if (devices.isEmpty()) {
            isSearching = false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dispositivos DLNA",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = {
                            isSearching = true
                            devices.clear()
                            DlnaDeviceManager.refreshDevices()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rotate_ccw),
                            contentDescription = "Refresh"
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_x),
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Device list or loading indicator
                if (isSearching && devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Buscando dispositivos DLNA...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No se encontraron dispositivos DLNA",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Asegúrate de que tu TV está encendida y conectada a la misma red WiFi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(devices) { device ->
                            DlnaDeviceItem(
                                device = device,
                                onClick = {
                                    DlnaHelper.setCurrentDevice(device)
                                    onDeviceSelected()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DlnaDeviceItem(
    device: Device<*, *, *>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tv),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.details.friendlyName,
                style = MaterialTheme.typography.bodyLarge
            )
            
            device.details.manufacturerDetails?.manufacturer?.let { manufacturer ->
                Text(
                    text = manufacturer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
