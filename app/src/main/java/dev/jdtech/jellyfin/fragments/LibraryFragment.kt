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
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentLibraryBinding
import dev.jdtech.jellyfin.viewmodels.LibraryViewModelFactory
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding

    private val args: LibraryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModelFactory =
            LibraryViewModelFactory(requireNotNull(this.activity).application, args.libraryId)
        val viewModel: LibraryViewModel by viewModels { viewModelFactory }
        binding.viewModel = viewModel
        binding.itemsRecyclerView.adapter =
            ViewItemListAdapter(ViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            })
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        findNavController().navigate(
            LibraryFragmentDirections.actionLibraryFragmentToMediaInfoFragment(
                item.id,
                item.name
            )
        )
    }
}