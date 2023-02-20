package dev.jdtech.jellyfin.utils

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.media3.common.Player
import androidx.media3.ui.TimeBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import dev.jdtech.jellyfin.utils.bif.BifData
import dev.jdtech.jellyfin.utils.bif.BifUtil
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class PreviewScrubListener(
    private val scrubbingPreview: ImageView,
    private val timeBarView: View,
    private val player: Player,
    private val currentTrickPlay: StateFlow<BifData?>
) : TimeBar.OnScrubListener {

    private val roundedCorners = RoundedCorners(10)

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing started at $position")

        if (currentTrickPlay.value == null)
            return

        scrubbingPreview.visibility = View.VISIBLE
        onScrubMove(timeBar, position)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing to $position")

        val currentBifData = currentTrickPlay.value ?: return
        val image = BifUtil.getTrickPlayFrame(position.toInt(), currentBifData) ?: return

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

        Glide.with(scrubbingPreview)
            .load(image)
            .transform(roundedCorners)
            .into(scrubbingPreview)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        Timber.d("Scrubbing stopped at $position")

        scrubbingPreview.visibility = View.GONE
    }
}
