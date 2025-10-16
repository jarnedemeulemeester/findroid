package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.presentation.utils.plus
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun RootLayout(padding: PaddingValues = PaddingValues(), content: @Composable BoxScope.() -> Unit) {
    val safePadding = rememberSafePadding()

    val safePaddingValues = PaddingValues(
        start = safePadding.start,
        top = safePadding.top,
        end = safePadding.end,
        bottom = safePadding.bottom,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(safePaddingValues + padding),
        content = content,
    )
}
