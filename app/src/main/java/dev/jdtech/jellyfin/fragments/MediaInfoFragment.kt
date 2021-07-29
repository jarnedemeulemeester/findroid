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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

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

        val snackbar =
            Snackbar.make(
                binding.mainLayout,
                getString(R.string.error_loading_data),
                Snackbar.LENGTH_INDEFINITE
            )
        snackbar.setAction(getString(R.string.retry)) {
            viewModel.loadData(args.itemId, args.itemType)
        }

        binding.viewModel = viewModel

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error) {
                snackbar.show()
            }
        })

        viewModel.item.observe(viewLifecycleOwner, { item ->
            if (item.originalTitle != item.name) {
                binding.originalTitle.visibility = View.VISIBLE
            } else {
                binding.originalTitle.visibility = View.GONE
            }
            if (item.trailerCount != null && item.trailerCount!! < 1) {
                binding.trailerButton.visibility = View.GONE
            }
            binding.communityRating.visibility = when (item.communityRating != null) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        })

        viewModel.actors.observe(viewLifecycleOwner, { actors ->
            when (actors.isNullOrEmpty()) {
                false -> binding.actors.visibility = View.VISIBLE
                true -> binding.actors.visibility = View.GONE
            }
        })

        viewModel.navigateToPlayer.observe(viewLifecycleOwner, { mediaSource ->
            mediaSource.id?.let {
                navigateToPlayerActivity(
                    args.itemId,
                    it,
                    viewModel.item.value!!.userData!!.playbackPositionTicks.div(10000)
                )
            }
        })

        viewModel.played.observe(viewLifecycleOwner, {
            val drawable = when (it) {
                true -> R.drawable.ic_check_filled
                false -> R.drawable.ic_check
            }

            binding.checkButton.setImageResource(drawable)
        })

        viewModel.favorite.observe(viewLifecycleOwner, {
            val drawable = when (it) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }

            binding.favoriteButton.setImageResource(drawable)
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

        binding.playButton.setOnClickListener {
            if (args.itemType == "Movie") {
                if (!viewModel.mediaSources.value.isNullOrEmpty()) {
                    if (viewModel.mediaSources.value!!.size > 1) {
                        VideoVersionDialogFragment(viewModel).show(
                            parentFragmentManager,
                            "videoversiondialog"
                        )
                    } else {
                        navigateToPlayerActivity(
                            args.itemId,
                            viewModel.mediaSources.value!![0].id!!,
                            viewModel.item.value!!.userData!!.playbackPositionTicks.div(10000)
                        )
                    }
                }
            }
        }

        binding.checkButton.setOnClickListener {
            when (viewModel.played.value) {
                true -> viewModel.markAsUnplayed(args.itemId)
                false -> viewModel.markAsPlayed(args.itemId)
            }
        }

        binding.favoriteButton.setOnClickListener {
            when (viewModel.favorite.value) {
                true -> viewModel.unmarkAsFavorite(args.itemId)
                false -> viewModel.markAsFavorite(args.itemId)
            }
        }

        viewModel.loadData(args.itemId, args.itemType)
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

    private fun navigateToPlayerActivity(
        itemId: UUID,
        mediaSourceId: String,
        playbackPosition: Long
    ) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToPlayerActivity(
                itemId,
                mediaSourceId,
                playbackPosition
            )
        )
    }
}