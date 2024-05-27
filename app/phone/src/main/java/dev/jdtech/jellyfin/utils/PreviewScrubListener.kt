package dev.jdtech.jellyfin.utils

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.media3.common.Player
import androidx.media3.ui.TimeBar
import coil.load
import coil.transform.RoundedCornersTransformation
import dev.jdtech.jellyfin.models.Trickplay
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class PreviewScrubListener(
    private val scrubbingPreview: ImageView,
    private val timeBarView: View,
    private val player: Player,
) : TimeBar.OnScrubListener {
    var currentTrickPlay: Trickplay? = null
    private val roundedCorners = RoundedCornersTransformation(10f)
    private var currentBitMap: Bitmap? = null

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing started at $position")

        if (currentTrickPlay == null) {
            return
        }

        scrubbingPreview.visibility = View.VISIBLE
        onScrubMove(timeBar, position)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing to $position")

        val trickplay = currentTrickPlay ?: return
        val image = trickplay.images[position.div(trickplay.interval).toInt()]

        val parent = scrubbingPreview.parent as ViewGroup

        val offset = position.toFloat() / player.duration
        val minX = scrubbingPreview.left
        val maxX = parent.width - parent.paddingRight

        val startX = timeBarView.left + (timeBarView.right - timeBarView.left) * offset - scrubbingPreview.width / 2
        val endX = startX + scrubbingPreview.width

        val layoutX = when {
            startX >= minX && endX <= maxX -> startX
            startX < minX -> minX
            else -> maxX - scrubbingPreview.width
        }.toFloat()

        scrubbingPreview.x = layoutX

        if (currentBitMap != image) {
            scrubbingPreview.load(image) {
                dispatcher(Dispatchers.Main.immediate)
                transformations(roundedCorners)
            }
            currentBitMap = image
        }
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        Timber.d("Scrubbing stopped at $position")

        scrubbingPreview.visibility = View.GONE
    }
}
