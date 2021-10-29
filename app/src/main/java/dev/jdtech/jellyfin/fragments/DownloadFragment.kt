package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.DownloadEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.DownloadViewItemListAdapter
import dev.jdtech.jellyfin.adapters.DownloadsListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentDownloadBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.DownloadViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

@AndroidEntryPoint
class DownloadFragment : Fragment() {

    private lateinit var binding: FragmentDownloadBinding
    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.downloadsRecyclerView.adapter = DownloadsListAdapter(
            DownloadViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            }, DownloadEpisodeListAdapter.OnClickListener { item ->
                navigateToEpisodeBottomSheetFragment(item)
            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, { isFinished ->
            binding.loadingIndicator.visibility = if (isFinished) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                checkIfLoginRequired(error)
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.downloadsRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.downloadsRecyclerView.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(parentFragmentManager, "errordialog")
        }

        viewModel.downloadSections.observe(viewLifecycleOwner, { sections ->
            if (sections.isEmpty()) {
                binding.noDownloadsText.visibility = View.VISIBLE
            } else {
                binding.noDownloadsText.visibility = View.GONE
            }
        })

        return binding.root
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