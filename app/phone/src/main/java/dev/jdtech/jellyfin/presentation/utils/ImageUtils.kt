package dev.jdtech.jellyfin.presentation.utils

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.vanniktech.blurhash.BlurHash
import timber.log.Timber

/**
 * Appends Jellyfin-specific resize parameters to an image URI if it's a remote URL.
 */
@Composable
fun Uri?.withJellyfinResize(
    widthDp: Dp,
    heightDp: Dp,
    quality: Int = 80,
): Uri? {
    if (this == null || scheme?.startsWith("http") != true) return this

    val density = LocalDensity.current

    val targetWidthPx = with(density) { widthDp.toPx().toInt() }
    val targetHeightPx = with(density) { heightDp.toPx().toInt() }

    Timber.d("Jellyfin resize: ${targetWidthPx}x$targetHeightPx")

    return buildUpon()
        .appendQueryParameter("fillWidth", targetWidthPx.toString())
        .appendQueryParameter("fillHeight", targetHeightPx.toString())
        .appendQueryParameter("quality", quality.toString())
        .build()
}

/**
 * Creates a [BitmapPainter] from a BlurHash string.
 */
fun String?.toBlurHash(
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

/**
 * Appends the local files directory path to a URI if it doesn't have a scheme.
 */
fun Uri?.toLocalFilesUri(context: Context): Uri? {
    if (this == null || scheme != null) return this
    return Uri.Builder()
        .appendEncodedPath(context.filesDir.absolutePath)
        .appendEncodedPath(path)
        .build()
}
