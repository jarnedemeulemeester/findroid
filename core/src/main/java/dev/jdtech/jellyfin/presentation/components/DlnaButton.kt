package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.dlna.DlnaHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * DLNA button that shows device selection dialog
 */
@Composable
fun DlnaButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDevicePicker by remember { mutableStateOf(false) }
    var isDlnaActive by remember { mutableStateOf(false) }
    
    // Update DLNA active state periodically
    DisposableEffect(Unit) {
        var updateJob: Job? = null
        
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                isDlnaActive = DlnaHelper.isDlnaDeviceAvailable(context)
                delay(500) // Check every 500ms
            }
        }
        
        onDispose {
            updateJob?.cancel()
        }
    }
    
    if (isDlnaActive) {
        // Filled button when DLNA is active
        FilledIconButton(
            onClick = {
                // If already connected, stop DLNA
                DlnaHelper.stopDlna(context)
            },
            modifier = modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tv),
                contentDescription = "DLNA Active"
            )
        }
    } else {
        // Regular button when DLNA is inactive
        IconButton(
            onClick = {
                // Show device picker
                showDevicePicker = true
            },
            modifier = modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tv),
                contentDescription = "DLNA",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    
    if (showDevicePicker) {
        DlnaDevicePicker(
            onDeviceSelected = {
                // Device is already set in DlnaDevicePicker
                showDevicePicker = false
            },
            onDismiss = {
                showDevicePicker = false
            }
        )
    }
}

