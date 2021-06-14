package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.jdtech.jellyfin.adapters.CollectionListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaBinding
import dev.jdtech.jellyfin.viewmodels.MediaViewModel
import dev.jdtech.jellyfin.viewmodels.MediaViewModelFactory

class MediaFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application
        val binding = FragmentMediaBinding.inflate(inflater, container, false)
        val viewModelFactory = MediaViewModelFactory(application)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(MediaViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter = CollectionListAdapter()

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            if (it) {
                binding.loadingIncicator.visibility = View.GONE
            }
        })

        return binding.root
    }
}