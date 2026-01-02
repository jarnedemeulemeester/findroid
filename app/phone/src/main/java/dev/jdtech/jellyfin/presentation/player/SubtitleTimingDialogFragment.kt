package dev.jdtech.jellyfin.presentation.player

import android.app.Dialog
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.player.local.R as PlayerR
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import java.lang.IllegalStateException

class SubtitleTimingDialogFragment(private val viewModel: PlayerViewModel) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            val view = activity.layoutInflater.inflate(R.layout.dialog_subtitle_timing, null)

            val offsetText = view.findViewById<TextView>(R.id.offset_text)
            val seekBar = view.findViewById<SeekBar>(R.id.offset_seekbar)
            val decrementButton = view.findViewById<TextView>(R.id.btn_decrement)
            val incrementButton = view.findViewById<TextView>(R.id.btn_increment)
            val resetButton = view.findViewById<TextView>(R.id.btn_reset)

            // SeekBar range: 0-300 (representing -15000ms to +15000ms in 100ms steps)
            seekBar.max = 300
            val currentOffset = viewModel.subtitleOffset.value
            seekBar.progress = ((currentOffset + 15000L) / 100).toInt()

            updateOffsetText(offsetText, currentOffset)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val offset = (progress * 100L) - 15000L
                        viewModel.setSubtitleOffset(offset)
                        updateOffsetText(offsetText, offset)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            decrementButton.setOnClickListener {
                val newOffset = viewModel.subtitleOffset.value - 100L
                viewModel.setSubtitleOffset(newOffset)
                seekBar.progress = ((newOffset.coerceIn(-15000L, 15000L) + 15000L) / 100).toInt()
                updateOffsetText(offsetText, viewModel.subtitleOffset.value)
            }

            incrementButton.setOnClickListener {
                val newOffset = viewModel.subtitleOffset.value + 100L
                viewModel.setSubtitleOffset(newOffset)
                seekBar.progress = ((newOffset.coerceIn(-15000L, 15000L) + 15000L) / 100).toInt()
                updateOffsetText(offsetText, viewModel.subtitleOffset.value)
            }

            resetButton.setOnClickListener {
                viewModel.setSubtitleOffset(0L)
                seekBar.progress = 150
                updateOffsetText(offsetText, 0L)
            }

            val builder = MaterialAlertDialogBuilder(activity)
            builder.setTitle(getString(PlayerR.string.subtitle_timing))
                .setView(view)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun updateOffsetText(textView: TextView, offsetMs: Long) {
        val sign = if (offsetMs >= 0) "+" else ""
        val seconds = offsetMs / 1000.0
        textView.text = String.format("%s%.1fs", sign, seconds)
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
