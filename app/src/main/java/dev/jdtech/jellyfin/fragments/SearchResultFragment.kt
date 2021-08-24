package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.FavoritesListAdapter
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSearchResultBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.SearchResultViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class SearchResultFragment : Fragment() {

    private lateinit var binding: FragmentSearchResultBinding
    private val viewModel: SearchResultViewModel by viewModels()

    private val args: SearchResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchResultBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.searchResultsRecyclerView.adapter = FavoritesListAdapter(
            ViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            }, HomeEpisodeListAdapter.OnClickListener { item ->
                navigateToEpisodeBottomSheetFragment(item)
            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, { isFinished ->
            binding.loadingIndicator.visibility = if (isFinished) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                checkIfLoginRequired(error)
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.searchResultsRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.searchResultsRecyclerView.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.query)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(parentFragmentManager, "errordialog")
        }

        viewModel.sections.observe(viewLifecycleOwner, { sections ->
            if (sections.isEmpty()) {
                binding.noSearchResultsText.visibility = View.VISIBLE
            } else {
                binding.noSearchResultsText.visibility = View.GONE
            }
        })

        viewModel.loadData(args.query)

        return binding.root
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        findNavController().navigate(
            FavoriteFragmentDirections.actionFavoriteFragmentToMediaInfoFragment(
                item.id,
                item.name,
                item.type ?: "Unknown"
            )
        )
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            FavoriteFragmentDirections.actionFavoriteFragmentToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }
}