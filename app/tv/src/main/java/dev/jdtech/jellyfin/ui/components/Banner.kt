package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import dev.jdtech.jellyfin.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
fun Banner() {
    Icon(
        painter = painterResource(id = R.drawable.ic_banner),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.width(320.dp)
    )
}
