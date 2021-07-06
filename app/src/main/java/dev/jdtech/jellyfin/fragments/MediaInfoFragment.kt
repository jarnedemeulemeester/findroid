package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class MediaInfoFragment : Fragment() {

    private lateinit var binding: FragmentMediaInfoBinding
    private val viewModel: MediaInfoViewModel by viewModels()

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

        binding.viewModel = viewModel

        viewModel.item.observe(viewLifecycleOwner, { item ->
            if (item.originalTitle != item.name) {
                binding.originalTitle.visibility = View.VISIBLE
            } else {
                binding.originalTitle.visibility = View.GONE
            }
            if (item.trailerCount != null && item.trailerCount!! < 1) {
                binding.trailerButton.visibility = View.GONE
            }
        })

        binding.trailerButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(viewModel.item.value?.remoteTrailers?.get(0)?.url)
            )
            startActivity(intent)
        }

        binding.nextUp.setOnClickListener {
            navigateToEpisodeBottomSheetFragment(viewModel.nextUp.value!!)
        }

        binding.seasonsRecyclerView.adapter =
            ViewItemListAdapter(ViewItemListAdapter.OnClickListener { season ->
                navigateToSeasonFragment(season)
            }, fixedWidth = true)
        binding.peopleRecyclerView.adapter = PersonListAdapter()

        viewModel.loadData(args.itemId)
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }

    private fun navigateToSeasonFragment(season: BaseItemDto) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToSeasonFragment(
                season.seriesId!!,
                season.id,
                season.seriesName,
                season.name
            )
        )
    }
}