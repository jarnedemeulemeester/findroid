package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.viewmodels.HomeViewModel

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter = ViewListAdapter(ViewListAdapter.OnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                    it.id,
                    it.name
                )
            )
        }, ViewItemListAdapter.OnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    it.id,
                    it.name
                )
            )
        }, HomeEpisodeListAdapter.OnClickListener {
            when (it.type) {
                "Episode" -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionNavigationHomeToEpisodeBottomSheetFragment(
                            it.id
                        )
                    )
                }
                "Movie" -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                            it.id,
                            it.name
                        )
                    )
                }
            }

        })

        binding.errorLayout.findViewById<View>(R.id.retry_button).setOnClickListener {
            viewModel.loadData()
        }

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            if (it) {
                binding.loadingIndicator.visibility = View.GONE
            } else {
                binding.loadingIndicator.visibility = View.VISIBLE
            }
        })

        viewModel.error.observe(viewLifecycleOwner, {
            if (it) {
                binding.errorLayout.visibility = View.VISIBLE
                binding.viewsRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.visibility = View.GONE
                binding.viewsRecyclerView.visibility = View.VISIBLE
            }
        })

        return binding.root
    }
}