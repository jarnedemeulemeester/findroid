package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModelFactory

class EpisodeBottomSheetFragment : BottomSheetDialogFragment() {
    private val args: EpisodeBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = EpisodeBottomSheetBinding.inflate(inflater, container, false)
        val viewModelFactory = EpisodeBottomSheetViewModelFactory(requireNotNull(this.activity).application, args.episodeId)
        val viewModel: EpisodeBottomSheetViewModel by viewModels { viewModelFactory }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }
}