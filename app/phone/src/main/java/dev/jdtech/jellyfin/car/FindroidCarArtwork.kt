package dev.jdtech.jellyfin.car

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import java.io.File

private const val MAX_CAR_ARTWORK_SIZE = 384

internal object FindroidCarArtwork {
    private val cache = LruCache<String, CarIcon>(64)

    fun iconFor(paths: List<String>): CarIcon? {
        paths.forEach { path ->
            iconFor(path)?.let { return it }
        }
        return null
    }

    fun iconFor(path: String?): CarIcon? {
        if (path.isNullOrBlank()) return null
        cache.get(path)?.let { return it }

        val file = File(path)
        if (!file.isFile) return null

        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val bitmap =
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
                },
            ) ?: return null

        val icon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
        cache.put(path, icon)
        return icon
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        val longestSide = maxOf(width, height)
        while (longestSide / sample > MAX_CAR_ARTWORK_SIZE) {
            sample *= 2
        }
        return sample
    }
}
