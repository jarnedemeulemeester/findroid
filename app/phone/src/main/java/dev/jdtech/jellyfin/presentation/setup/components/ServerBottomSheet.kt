package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.setup.R as SetupR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerBottomSheet(
    name: String,
    address: String,
    onAddresses: () -> Unit,
    onRemoveServer: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            ServerItem(
                name = name,
                address = address,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacings.medium)
                    .fillMaxWidth(),
            )
            Column {
                ServerBottomSheetItem(
                    icon = painterResource(CoreR.drawable.ic_globe),
                    text = stringResource(SetupR.string.addresses),
                    onClick = onAddresses,
                )
                ServerBottomSheetItem(
                    icon = painterResource(CoreR.drawable.ic_trash),
                    text = stringResource(SetupR.string.remove_server_dialog),
                    onClick = onRemoveServer,
                )
            }
        }
    }
}

@Composable
private fun ServerBottomSheetItem(
    icon: Painter,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
            )
            .padding(MaterialTheme.spacings.medium),
    ) {
        Icon(painter = icon, contentDescription = null)
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ServerBottomSheetPreview() {
    FindroidTheme {
        ServerBottomSheet(
            name = "Jellyfin Server",
            address = "http://192.168.0.10:8096",
            onAddresses = {},
            onRemoveServer = {},
            onDismissRequest = {},
            sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerBottomSheetItemPreview() {
    FindroidTheme {
        ServerBottomSheetItem(
            icon = painterResource(CoreR.drawable.ic_globe),
            text = stringResource(SetupR.string.addresses),
            onClick = {},
        )
    }
}
