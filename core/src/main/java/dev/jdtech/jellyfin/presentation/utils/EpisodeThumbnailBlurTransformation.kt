package dev.jdtech.jellyfin.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil3.size.Size
import coil3.transform.Transformation

class EpisodeThumbnailBlurTransformation(context: Context) : Transformation() {
    private val context = context.applicationContext

    override val cacheKey: String = "episode_thumbnail_blur"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val source =
            if (input.config == Bitmap.Config.ARGB_8888) input
            else input.copy(Bitmap.Config.ARGB_8888, false) ?: return input
        val blurredInput =
            Bitmap.createScaledBitmap(
                source,
                (source.width * DOWNSCALE_FACTOR).toInt().coerceAtLeast(1),
                (source.height * DOWNSCALE_FACTOR).toInt().coerceAtLeast(1),
                true,
            )
        val output =
            Bitmap.createBitmap(blurredInput.width, blurredInput.height, Bitmap.Config.ARGB_8888)
        val renderScript = RenderScript.create(context)
        val inputAllocation = Allocation.createFromBitmap(renderScript, blurredInput)
        val outputAllocation = Allocation.createFromBitmap(renderScript, output)
        val blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

        try {
            blur.setRadius(MAX_BLUR_RADIUS)
            blur.setInput(inputAllocation)
            blur.forEach(outputAllocation)
            outputAllocation.copyTo(output)
        } finally {
            blur.destroy()
            inputAllocation.destroy()
            outputAllocation.destroy()
            renderScript.destroy()
        }

        val result = Bitmap.createScaledBitmap(output, source.width, source.height, true)
        output.recycle()
        if (blurredInput !== source) blurredInput.recycle()
        return result
    }

    private companion object {
        const val MAX_BLUR_RADIUS = 25f
        const val DOWNSCALE_FACTOR = 0.25f
    }
}
