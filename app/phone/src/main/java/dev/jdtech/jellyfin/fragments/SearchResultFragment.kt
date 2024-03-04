package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.FavoritesListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSearchResultBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.utils.playerErrorDialogSnackbar
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.viewmodels.SearchResultViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SearchResultFragment : Fragment() {

    private lateinit var binding: FragmentSearchResultBinding
    private val viewModel: SearchResultViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val args: SearchResultFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSearchResultBinding.inflate(inflater, container, false)

        binding.searchResultsRecyclerView.adapter = FavoritesListAdapter(
            onItemClickListener = { item -> navigateToMediaItem(item) },
            onItemLongClickListener = { item ->
                binding.loadingIndicator.isVisible = true
                playerViewModel.loadPlayerItems(item)
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is SearchResultViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is SearchResultViewModel.UiState.Loading -> bindUiStateLoading()
                        is SearchResultViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadData(args.query)
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.query)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
            }
            binding.loadingIndicator.isVisible = false
        }

        return binding.root
    }

    private fun bindPlayerItemsError(error: PlayerViewModel.PlayerItemError) {
        Timber.e(error.error.message)
        playerErrorDialogSnackbar(parentFragmentManager, binding.root, error)
    }

    private fun bindPlayerItems(items: PlayerViewModel.PlayerItems) {
        navigateToPlayerActivity(items.items.toTypedArray())
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
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.searchResultsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    SearchResultFragmentDirections.actionSearchResultFragmentToMovieFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidShow -> {
                findNavController().navigate(
                    SearchResultFragmentDirections.actionSearchResultFragmentToShowFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidEpisode -> {
                findNavController().navigate(
                    SearchResultFragmentDirections.actionSearchResultFragmentToEpisodeBottomSheetFragment(
                        item.id,
                    ),
                )
            }
        }
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            SearchResultFragmentDirections.actionSearchResultFragmentToPlayerActivity(
                playerItems,
            ),
        )
    }
}
