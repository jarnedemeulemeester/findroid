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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jdtech.jellyfin.roku.RokuDevice
import dev.jdtech.jellyfin.roku.RokuHelper
import kotlinx.coroutines.launch
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun RokuDevicePicker(
    onDeviceSelected: (RokuDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var devices by remember { mutableStateOf<List<RokuDevice>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            devices = RokuHelper.discoverRokuDevices(context)
            isScanning = false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Seleccionar dispositivo Roku",
                    style = MaterialTheme.typography.headlineSmall,
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Buscando dispositivos Roku...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_tv),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No se encontraron dispositivos Roku",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Asegúrate de que tu Roku esté encendido\ny conectado a la misma red WiFi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    ) {
                        items(devices) { device ->
                            RokuDeviceItem(
                                device = device,
                                onClick = {
                                    RokuHelper.setCurrentDevice(device)
                                    onDeviceSelected(device)
                                },
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    
                    if (!isScanning) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                isScanning = true
                                coroutineScope.launch {
                                    devices = RokuHelper.discoverRokuDevices(context)
                                    isScanning = false
                                }
                            },
                        ) {
                            Text("Buscar de nuevo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RokuDeviceItem(
    device: RokuDevice,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_tv),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (device.modelName.isNotEmpty()) {
                    Text(
                        text = device.modelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = device.ipAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
