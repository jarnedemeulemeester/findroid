package dev.jdtech.jellyfin.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModelFactory

class MediaInfoFragment : Fragment() {

    private lateinit var binding: FragmentMediaInfoBinding
    private lateinit var viewModel: MediaInfoViewModel

    private val args: MediaInfoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaInfoBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModelFactory = MediaInfoViewModelFactory(requireNotNull(this.activity).application, args.itemId)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MediaInfoViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.item.observe(viewLifecycleOwner, {
            if (it.originalTitle != it.name) {
                binding.originalTitle.visibility = View.VISIBLE
            } else {
                binding.originalTitle.visibility = View.GONE
            }
        })
    }

}