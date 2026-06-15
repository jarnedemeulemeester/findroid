package dev.jdtech.jellyfin.core.presentation.utils

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.vanniktech.blurhash.BlurHash
import java.io.File

/**
 * Processes and optimizes an image URI for display:
 * - If local (no scheme), it resolves the path relative to the app's internal files' directory.
 * - If remote (http/https), it appends resizing query parameters based on the target dimensions.
 */
@Composable
fun Uri?.toOptimizedImageUri(
    widthDp: Dp? = null,
    heightDp: Dp? = null,
    quality: Int = 80,
): Uri? {
    if (this == null) return null
    val context = LocalContext.current
    val density = LocalDensity.current

    return remember(this, widthDp, heightDp, quality) {
        val baseUri = if (scheme == null) {
            Uri.fromFile(File(context.filesDir, path ?: ""))
        } else {
            this
        }

        if (baseUri.scheme?.startsWith("http") == true && widthDp != null && heightDp != null) {
            val targetWidthPx = with(density) { widthDp.toPx().toInt() }
            val targetHeightPx = with(density) { heightDp.toPx().toInt() }

            baseUri.buildUpon()
                .appendQueryParameter("fillWidth", targetWidthPx.toString())
                .appendQueryParameter("fillHeight", targetHeightPx.toString())
                .appendQueryParameter("quality", quality.toString())
                .build()
        } else {
            baseUri
        }
    }
}

/**
 * Creates a [BitmapPainter] from a BlurHash string.
 */
fun String?.toBlurHashPainter(
    width: Int = 25,
    height: Int = 25,
): BitmapPainter? {
    if (!this.isNullOrEmpty()) {
        val bitmap = BlurHash.decode(
            blurHash = this,
            width = width,
            height = height,
        )
        return bitmap?.asImageBitmap()?.let { BitmapPainter(it) }
    } else {
        return null
    }
}
