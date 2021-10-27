package dev.jdtech.jellyfin.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentLibraryBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.SortDialogFragment
import dev.jdtech.jellyfin.utils.SortBy
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.SortOrder
import javax.inject.Inject

@AndroidEntryPoint
class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private val viewModel: LibraryViewModel by viewModels()

    private val args: LibraryFragmentArgs by navArgs()

    @Inject
    lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.library_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by -> {
                SortDialogFragment(args.libraryId, args.libraryType, viewModel, "sortBy").show(
                    parentFragmentManager,
                    "sortdialog"
                )
                true
            }
            R.id.action_sort_order -> {
                SortDialogFragment(args.libraryId, args.libraryType, viewModel, "sortOrder").show(
                    parentFragmentManager,
                    "sortdialog"
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
                checkIfLoginRequired(error)
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.itemsRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.itemsRecyclerView.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadItems(args.libraryId, args.libraryType)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(
                parentFragmentManager,
                "errordialog"
            )
        }

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        binding.itemsRecyclerView.adapter =
            ViewItemListAdapter(ViewItemListAdapter.OnClickListener { item ->
                if (args.libraryType == "boxsets") {
                    navigateToLibraryFragment(item)
                } else {
                    navigateToMediaInfoFragment(item)
                }
            })

        // Sorting options
        val sortBy = SortBy.fromString(sp.getString("sortBy", SortBy.defaultValue.name)!!)
        val sortOrder = try {
            SortOrder.valueOf(sp.getString("sortOrder", SortOrder.ASCENDING.name)!!)
        } catch (e: IllegalArgumentException) {
            SortOrder.ASCENDING
        }

        viewModel.loadItems(args.libraryId, args.libraryType, sortBy = sortBy, sortOrder = sortOrder)
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

    private fun navigateToLibraryFragment(item: BaseItemDto) {
        findNavController().navigate(
            LibraryFragmentDirections.actionLibraryFragmentToLibraryFragment(
                item.id,
                item.name,
                item.type ?: "Unknown"
            )
        )
    }
}