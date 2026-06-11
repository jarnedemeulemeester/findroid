package dev.jdtech.jellyfin.presentation.utils

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import timber.log.Timber

val LocalImageQuality = compositionLocalOf<String> { error("Image Quality not defined") }

/**
 * Appends Jellyfin-specific resize parameters to an image URI if it's a remote URL.
 */
fun Uri?.withJellyfinResize(
    width: Int,
    height: Int,
    quality: Int = 80,
    imageQuality: String = "default",
): Uri? {
    if (this == null || scheme?.startsWith("http") != true) return this

    var finalWidth = width
    var finalHeight = height
    var finalQuality = quality

    Timber.d("Loading image with quality: $imageQuality")

    when (imageQuality) {
            "tiny" -> {
                finalWidth /= 3
                finalHeight /= 3
                finalQuality = 50
            }
            "small" -> {
                finalWidth /= 2
                finalHeight /= 2
            }
            "large" -> {
                finalWidth = (finalWidth * 1.5f).toInt()
                finalHeight = (finalHeight * 1.5f).toInt()
                finalQuality = 90
            }
            "original" -> return this
        }

    return buildUpon()
        .appendQueryParameter("fillWidth", finalWidth.toString())
        .appendQueryParameter("fillHeight", finalHeight.toString())
        .appendQueryParameter("quality", finalQuality.toString())
        .build()
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
