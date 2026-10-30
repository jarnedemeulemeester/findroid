package dev.jdtech.jellyfin.presentation.player

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.player.local.R
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.lang.IllegalStateException
import java.text.DecimalFormat

class QualitySelectionDialogFragment(private val viewModel: PlayerViewModel) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bitrates =
            if (viewModel.canTranscodeVideo) {
                listOf(null) + viewModel.getVideoQualityOptions()
            } else {
                listOf(null)
            }
        val qualityTexts = bitrates.map { bitrate ->
            when {
                bitrate == null -> getString(R.string.video_quality_best)
                bitrate >= 1_000_000 ->
                    getString(
                        R.string.video_quality_mbps,
                        DecimalFormat("0.#").format(bitrate / 1_000_000.0),
                    )
                else -> getString(R.string.video_quality_kbps, bitrate / 1000)
            }
        }

        return activity?.let { activity ->
            val builder = MaterialAlertDialogBuilder(activity)
            builder.setTitle(getString(R.string.select_video_quality)).setSingleChoiceItems(
                qualityTexts.toTypedArray(),
                getCheckedItem(bitrates),
            ) { dialog, which ->
                viewModel.setMaxStreamingBitrate(bitrates[which])
                dialog.dismiss()
            }
            if (!viewModel.canTranscodeVideo) {
                builder.setView(createTranscodingDisabledView(activity))
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // The saved bitrate can be missing from the list, check the highest option within it
    private fun getCheckedItem(bitrates: List<Int?>): Int {
        if (!viewModel.canTranscodeVideo) return 0
        val maxStreamingBitrate = viewModel.maxStreamingBitrate ?: return 0
        return bitrates.indexOfFirst { it != null && it <= maxStreamingBitrate }
    }

    private fun createTranscodingDisabledView(context: Context): TextView {
        val textAppearance = TypedValue()
        context.theme.resolveAttribute(
            com.google.android.material.R.attr.textAppearanceBodyMedium,
            textAppearance,
            true,
        )
        val padding = (24 * resources.displayMetrics.density).toInt()
        return TextView(context).apply {
            text = getString(R.string.video_quality_transcoding_disabled)
            TextViewCompat.setTextAppearance(this, textAppearance.resourceId)
            setTextColor(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    currentTextColor,
                )
            )
            setPadding(padding, 0, padding, padding)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fix for hiding the system bars on API < 30
        activity?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
