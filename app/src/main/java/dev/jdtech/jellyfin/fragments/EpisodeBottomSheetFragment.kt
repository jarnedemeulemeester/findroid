package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import java.util.*

@AndroidEntryPoint
class EpisodeBottomSheetFragment : BottomSheetDialogFragment() {
    private val args: EpisodeBottomSheetFragmentArgs by navArgs()

    private lateinit var binding: EpisodeBottomSheetBinding
    private val viewModel: EpisodeBottomSheetViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EpisodeBottomSheetBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.playButton.setOnClickListener {
            viewModel.mediaSources.value?.get(0)?.id?.let { mediaSourceId ->
                navigateToPlayerActivity(args.episodeId,
                    mediaSourceId
                )
            }
        }

        viewModel.item.observe(viewLifecycleOwner, { episode ->
            if (episode.userData?.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.userData?.playedPercentage?.times(1.26))!!.toFloat(),
                    context?.resources?.displayMetrics
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }
        })

        viewModel.loadEpisode(args.episodeId)

        return binding.root
    }

    private fun navigateToPlayerActivity(itemId: UUID, mediaSourceId: String) {
        findNavController().navigate(
            EpisodeBottomSheetFragmentDirections.actionEpisodeBottomSheetFragmentToPlayerActivity(
                itemId,
                mediaSourceId
            )
        )
    }
}