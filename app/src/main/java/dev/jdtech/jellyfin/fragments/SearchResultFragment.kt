package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber

@AndroidEntryPoint
class SearchResultFragment : Fragment() {

    private lateinit var binding: FragmentSearchResultBinding
    private val viewModel: SearchResultViewModel by viewModels()
    private val args: SearchResultFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchResultBinding.inflate(inflater, container, false)

        binding.searchResultsRecyclerView.adapter = FavoritesListAdapter(
            ViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            }, HomeEpisodeListAdapter.OnClickListener { item ->
                navigateToEpisodeBottomSheetFragment(item)
            })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is SearchResultViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is SearchResultViewModel.UiState.Loading -> bindUiStateLoading()
                        is SearchResultViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
                viewModel.loadData(args.query)
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.query)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, "errordialog")
        }


        return binding.root
    }

    private fun bindUiStateNormal(uiState: SearchResultViewModel.UiState.Normal) {
        uiState.apply {
            binding.noSearchResultsText.isVisible = sections.isEmpty()

            val adapter = binding.searchResultsRecyclerView.adapter as FavoritesListAdapter
            adapter.submitList(uiState.sections)
        }
        binding.loadingIndicator.isVisible = false
        binding.searchResultsRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: SearchResultViewModel.UiState.Error) {
        val error = uiState.message ?: getString(R.string.unknown_error)
        errorDialog = ErrorDialogFragment(error)
        binding.loadingIndicator.isVisible = false
        binding.searchResultsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(error)
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