package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter = ViewListAdapter(ViewListAdapter.OnClickListener {
            navigateToLibraryFragment(it)
        }, ViewItemListAdapter.OnClickListener {
            navigateToMediaInfoFragment(it)
        }, HomeEpisodeListAdapter.OnClickListener { item ->
            when (item.type) {
                "Episode" -> {
                    navigateToEpisodeBottomSheetFragment(item)
                }
                "Movie" -> {
                    navigateToMediaInfoFragment(item)
                }
            }

        })

        binding.errorLayout.findViewById<View>(R.id.retry_button).setOnClickListener {
            viewModel.loadData()
        }

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
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

    private fun navigateToLibraryFragment(view: dev.jdtech.jellyfin.models.View) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                view.id,
                view.name
            )
        )
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                item.id,
                item.name
            )
        )
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }
}