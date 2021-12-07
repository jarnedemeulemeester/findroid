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
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.*
import dev.jdtech.jellyfin.databinding.FragmentDownloadBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.DownloadViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class DownloadFragment : Fragment() {

    private lateinit var binding: FragmentDownloadBinding
    private val viewModel: DownloadViewModel by viewModels()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(inflater, container, false)

        binding.downloadsRecyclerView.adapter = DownloadsListAdapter(
            DownloadViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            }, DownloadEpisodeListAdapter.OnClickListener { item ->
                navigateToEpisodeBottomSheetFragment(item)
            })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is DownloadViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is DownloadViewModel.UiState.Loading -> bindUiStateLoading()
                        is DownloadViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, "errordialog")
        }

        return binding.root
    }

    private fun bindUiStateNormal(uiState: DownloadViewModel.UiState.Normal) {
        uiState.apply {
            binding.noDownloadsText.isVisible = downloadSections.isEmpty()

            val adapter = binding.downloadsRecyclerView.adapter as DownloadsListAdapter
            adapter.submitList(downloadSections)
        }
        binding.loadingIndicator.isVisible = false
        binding.downloadsRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: DownloadViewModel.UiState.Error) {
        val error = uiState.message ?: resources.getString(R.string.unknown_error)
        errorDialog = ErrorDialogFragment(error)
        binding.loadingIndicator.isVisible = false
        binding.downloadsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(error)
    }

    private fun navigateToMediaInfoFragment(item: PlayerItem) {
        findNavController().navigate(
            DownloadFragmentDirections.actionDownloadFragmentToMediaInfoFragment(
                UUID.randomUUID(),
                item.name,
                item.metadata?.type ?: "Unknown",
                item,
                isOffline = true
            )
        )
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: PlayerItem) {
        findNavController().navigate(
            DownloadFragmentDirections.actionDownloadFragmentToEpisodeBottomSheetFragment(
                UUID.randomUUID(),
                episode,
                isOffline = true
            )
        )
    }
}