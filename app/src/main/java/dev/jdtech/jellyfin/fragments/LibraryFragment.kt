package dev.jdtech.jellyfin.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentLibraryBinding
import dev.jdtech.jellyfin.viewmodels.LibraryViewModelFactory

class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private lateinit var viewModel: LibraryViewModel

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
        viewModel = ViewModelProvider(this, viewModelFactory).get(LibraryViewModel::class.java)
        binding.viewModel = viewModel
        binding.itemsRecyclerView.adapter =
            ViewItemListAdapter(ViewItemListAdapter.OnClickListener {
                findNavController().navigate(
                    LibraryFragmentDirections.actionLibraryFragmentToMediaInfoFragment(
                        it.id,
                        it.name
                    )
                )
            })
    }
}