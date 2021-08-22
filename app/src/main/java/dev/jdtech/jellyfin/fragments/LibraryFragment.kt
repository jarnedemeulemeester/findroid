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
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentLibraryBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private val viewModel: LibraryViewModel by viewModels()

    private val args: LibraryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.itemsRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.itemsRecyclerView.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadItems(args.libraryId)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(parentFragmentManager, "errordialog")
        }

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        binding.itemsRecyclerView.adapter =
            ViewItemListAdapter(ViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            })
        viewModel.loadItems(args.libraryId)
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        findNavController().navigate(
            LibraryFragmentDirections.actionLibraryFragmentToMediaInfoFragment(
                item.id,
                item.name,
                item.type ?: "Unknown"
            )
        )
    }
}