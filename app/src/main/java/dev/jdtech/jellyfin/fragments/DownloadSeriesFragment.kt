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
import dev.jdtech.jellyfin.adapters.DownloadEpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentDownloadSeriesBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.viewmodels.DownloadSeriesViewModel
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class DownloadSeriesFragment : Fragment() {

    private lateinit var binding: FragmentDownloadSeriesBinding
    private val viewModel: DownloadSeriesViewModel by viewModels()

    private lateinit var errorDialog: ErrorDialogFragment

    private val args: DownloadSeriesFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadSeriesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        binding.episodesRecyclerView.adapter =
            DownloadEpisodeListAdapter(DownloadEpisodeListAdapter.OnClickListener { episode ->
                navigateToEpisodeBottomSheetFragment(episode)
            }, args.seriesMetadata)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is DownloadSeriesViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is DownloadSeriesViewModel.UiState.Loading -> bindUiStateLoading(uiState)
                        is DownloadSeriesViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadEpisodes(args.seriesMetadata)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, "errordialog")
        }

        viewModel.loadEpisodes(args.seriesMetadata)
    }

    private fun bindUiStateNormal(uiState: DownloadSeriesViewModel.UiState.Normal) {
        val adapter = binding.episodesRecyclerView.adapter as DownloadEpisodeListAdapter
        adapter.submitList(uiState.downloadEpisodes)
        binding.episodesRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading(uiState: DownloadSeriesViewModel.UiState.Loading) {}

    private fun bindUiStateError(uiState: DownloadSeriesViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment(uiState.error)
        binding.episodesRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: PlayerItem) {
        findNavController().navigate(
            DownloadSeriesFragmentDirections.actionDownloadSeriesFragmentToEpisodeBottomSheetFragment(
                UUID.randomUUID(),
                episode,
                isOffline = true
            )
        )
    }
}