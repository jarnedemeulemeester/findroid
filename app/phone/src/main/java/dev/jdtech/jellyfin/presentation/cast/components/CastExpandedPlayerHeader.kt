package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastExpandedPlayerHeader(
    deviceName: String?,
    onClose: () -> Unit,
    onDeviceClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(painterResource(CoreR.drawable.ic_x), contentDescription = "Close")
        }
        
        // Device Pill
        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF81C784),
            onClick = onDeviceClick
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(CoreR.drawable.ic_cast), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(deviceName ?: "No Device", style = MaterialTheme.typography.labelLarge, color = Color.Black)
            }
        }
    }
}

@Preview
@Composable
private fun CastExpandedPlayerHeaderPreview() {
    FindroidTheme {
        CastExpandedPlayerHeader(
            deviceName = "Test Device",
            onClose = {},
            onDeviceClick = {}
        )
    }
}
