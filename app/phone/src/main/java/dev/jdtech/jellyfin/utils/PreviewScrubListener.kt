package dev.jdtech.jellyfin.utils

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.media3.common.Player
import androidx.media3.ui.TimeBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import dev.jdtech.jellyfin.utils.bif.BifData
import dev.jdtech.jellyfin.utils.bif.BifUtil
import timber.log.Timber

class PreviewScrubListener(
    private val previewFrameLayout: View,
    private val scrubbingPreview: ImageView,
    private val timeBarView: View,
    private val player: Player,
    private val currentTrickPlay: LiveData<BifData?>
) : TimeBar.OnScrubListener {

    private val roundedCorners = RoundedCorners(10)

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing started at $position")

        if (currentTrickPlay.value == null)
            return

        previewFrameLayout.visibility = View.VISIBLE
        onScrubMove(timeBar, position)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing to $position")

        val currentBifData = currentTrickPlay.value ?: return
        val image = BifUtil.getTrickPlayFrame(position.toInt(), currentBifData) ?: return

        val parent = previewFrameLayout.parent as ViewGroup
        val layoutParams = previewFrameLayout.layoutParams as ViewGroup.MarginLayoutParams

        val offset = position.toFloat() / player.duration
        val minX = previewFrameLayout.left
        val maxX = parent.width - parent.paddingRight - layoutParams.rightMargin

        val startX = timeBarView.left + (timeBarView.right - timeBarView.left) * offset - previewFrameLayout.width / 2
        val endX = startX + previewFrameLayout.width

        val layoutX = when {
            startX >= minX && endX <= maxX -> startX
            startX < minX -> minX
            else -> maxX - previewFrameLayout.width
        }.toFloat()

        previewFrameLayout.x = layoutX

        Glide.with(scrubbingPreview)
            .load(image)
            .transform(roundedCorners)
            .into(scrubbingPreview)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        Timber.d("Scrubbing stopped at $position")

        previewFrameLayout.visibility = View.GONE
    }
}
