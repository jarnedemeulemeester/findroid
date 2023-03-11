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
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.FavoritesListAdapter
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentDownloadsBinding
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.viewmodels.DownloadsViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class DownloadsFragment : Fragment() {
    private lateinit var binding: FragmentDownloadsBinding
    private val viewModel: DownloadsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadsBinding.inflate(inflater, container, false)

        binding.downloadsRecyclerView.adapter = FavoritesListAdapter(
            ViewItemListAdapter.OnClickListener { item ->
                navigateToMediaItem(item)
            },
            HomeEpisodeListAdapter.OnClickListener { item ->
                navigateToMediaItem(item)
            }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is DownloadsViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is DownloadsViewModel.UiState.Loading -> bindUiStateLoading()
                        is DownloadsViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        return binding.root
    }

    private fun bindUiStateNormal(uiState: DownloadsViewModel.UiState.Normal) {
        binding.loadingIndicator.isVisible = false
        binding.downloadsRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
        binding.noDownloadsText.isVisible = uiState.sections.isEmpty()
        val adapter = binding.downloadsRecyclerView.adapter as FavoritesListAdapter
        adapter.submitList(uiState.sections)
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    DownloadsFragmentDirections.actionDownloadsFragmentToMovieFragment(
                        item.id,
                        item.name
                    )
                )
            }
            is FindroidShow -> {
                findNavController().navigate(
                    DownloadsFragmentDirections.actionDownloadsFragmentToShowFragment(
                        item.id,
                        item.name
                    )
                )
            }
        }
    }
}
