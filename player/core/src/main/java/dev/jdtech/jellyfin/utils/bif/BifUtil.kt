package dev.jdtech.jellyfin.utils.bif

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.nio.ByteBuffer

object BifUtil {

    /* https://github.com/nicknsy/jellyscrub/blob/main/Nick.Plugin.Jellyscrub/Api/trickplay.js */
    private val BIF_MAGIC_NUMBERS = byteArrayOf(0x89.toByte(), 0x42.toByte(), 0x49.toByte(), 0x46.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
    private const val SUPPORTED_BIF_VERSION = 0

    fun trickPlayDecode(array: ByteArray, width: Int): BifData? {
        val data = Indexed8Array(array)

        Timber.d("BIF file size: ${data.limit()}")

        for (b in BIF_MAGIC_NUMBERS) {
            if (data.read() != b) {
                Timber.d("Attempted to read invalid bif file.")
                return null
            }
        }

        val bifVersion = data.readInt32()
        if (bifVersion != SUPPORTED_BIF_VERSION) {
            Timber.d("Client only supports BIF v$SUPPORTED_BIF_VERSION but file is v$bifVersion")
            return null
        }

        Timber.d("BIF version: $bifVersion")

        val bifImgCount = data.readInt32()
        if (bifImgCount <= 0) {
            Timber.d("BIF file contains no images.")
            return null
        }

        Timber.d("BIF image count: $bifImgCount")

        var timestampMultiplier = data.readInt32()
        if (timestampMultiplier == 0) timestampMultiplier = 1000

        data.addPosition(44) // Reserved

        val bifIndex = mutableListOf<BifIndexEntry>()
        for (i in 0 until bifImgCount) {
            bifIndex.add(BifIndexEntry(data.readInt32(), data.readInt32()))
        }

        val bifImages = mutableMapOf<Int, Bitmap>()
        for (i in bifIndex.indices) {
            val indexEntry = bifIndex[i]
            val timestamp = indexEntry.timestamp
            val offset = indexEntry.offset
            val nextOffset = bifIndex.getOrNull(i + 1)?.offset ?: data.limit()

            val imageBuffer = ByteBuffer.wrap(data.array(), offset, nextOffset - offset).order(data.order())
            val imageBytes = ByteArray(imageBuffer.remaining())
            imageBuffer.get(imageBytes)

            val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            bifImages[timestamp] = bmp
        }

        return BifData(bifVersion, timestampMultiplier, bifImgCount, bifImages, width)
    }

    fun getTrickPlayFrame(playerTimestamp: Int, data: BifData): Bitmap? {
        val multiplier = data.timestampMultiplier
        val images = data.images

        val frame = playerTimestamp / multiplier
        return images[frame]
    }
}
