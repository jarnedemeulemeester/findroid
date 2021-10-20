package dev.jdtech.jellyfin.tv.ui

import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.leanback.widget.Presenter
import dev.jdtech.jellyfin.databinding.DynamicMediaItemBinding
import dev.jdtech.jellyfin.databinding.MediaItemBinding
import org.jellyfin.sdk.model.api.BaseItemDto

class MediaItemPresenter(private val onClick: (BaseItemDto) -> Unit) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val mediaView =
            MediaItemBinding
                .inflate(parent.context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .root
        return ViewHolder(mediaView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        if (item is BaseItemDto) {
            DataBindingUtil.getBinding<MediaItemBinding>(viewHolder.view)?.apply {
                itemDto = item
                viewHolder.view.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}

class DynamicMediaItemPresenter(private val onClick: (BaseItemDto) -> Unit): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val mediaView =
            DynamicMediaItemBinding
                .inflate(parent.context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .root
        return ViewHolder(mediaView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        if (item is BaseItemDto) {
            DataBindingUtil.getBinding<DynamicMediaItemBinding>(viewHolder.view)?.apply {
                itemDto = item
                viewHolder.view.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}