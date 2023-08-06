package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.bindPersonImage
import dev.jdtech.jellyfin.databinding.PersonItemBinding
import org.jellyfin.sdk.model.api.BaseItemPerson

class PersonListAdapter(private val clickListener: (item: BaseItemPerson) -> Unit) : ListAdapter<BaseItemPerson, PersonListAdapter.PersonViewHolder>(DiffCallback) {

    class PersonViewHolder(private var binding: PersonItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(person: BaseItemPerson) {
            binding.personName.text = person.name
            binding.personRole.text = person.role
            bindPersonImage(binding.personImage, person)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BaseItemPerson>() {
        override fun areItemsTheSame(oldItem: BaseItemPerson, newItem: BaseItemPerson): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BaseItemPerson, newItem: BaseItemPerson): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        return PersonViewHolder(
            PersonItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { clickListener(item) }
    }
}
