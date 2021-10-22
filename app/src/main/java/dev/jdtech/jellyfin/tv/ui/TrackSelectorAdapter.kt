package dev.jdtech.jellyfin.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel

class TrackSelectorAdapter(
    private val items: List<Track>,
    private val viewModel: PlayerActivityViewModel,
    private val trackType: String,
    private val dismissWindow: () -> Unit
) : RecyclerView.Adapter<TrackSelectorAdapter.TrackSelectorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackSelectorViewHolder {
        return TrackSelectorViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TrackSelectorViewHolder, position: Int) {
        holder.bind(items[position], viewModel, trackType, dismissWindow)
    }

    override fun getItemCount(): Int = items.size

    class TrackSelectorViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(
            item: Track,
            viewModel: PlayerActivityViewModel,
            trackType: String,
            dismissWindow: () -> Unit
        ) {
            view.findViewById<Button>(R.id.track_name).apply {
                text = String.format(
                    view.resources.getString(R.string.track_selection),
                    item.language,
                    item.title,
                    item.codec
                )
                setOnClickListener {
                    when (trackType) {
                        TrackType.AUDIO -> viewModel.switchToTrack(
                            TrackType.AUDIO,
                            item.playerTrack
                        )
                        TrackType.SUBTITLE -> viewModel.switchToTrack(
                            TrackType.SUBTITLE,
                            item.playerTrack
                        )
                    }
                    dismissWindow()
                }
            }
        }
    }

    data class Track(
        val title: String,
        val language: String,
        val codec: String,
        val selected: Boolean,
        val playerTrack: MPVPlayer.Companion.Track
    )
}