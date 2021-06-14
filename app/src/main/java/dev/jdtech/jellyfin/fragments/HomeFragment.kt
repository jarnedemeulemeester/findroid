package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import dev.jdtech.jellyfin.viewmodels.HomeViewModelFactory

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application
        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        val viewModelFactory = HomeViewModelFactory(application)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter = ViewListAdapter()

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            if (it) {
                binding.loadingIncicator.visibility = View.GONE
            }
        })

        return binding.root
    }
}