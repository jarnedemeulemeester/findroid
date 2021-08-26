package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
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
                binding.mediaInfoScrollview.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.mediaInfoScrollview.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.itemId, args.itemType)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(
                parentFragmentManager,
                "errordialog"
            )
        }

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

        viewModel.navigateToPlayer.observe(viewLifecycleOwner, { playerItems ->
            if (playerItems != null) {
                navigateToPlayerActivity(
                    playerItems
                )
                viewModel.doneNavigatingToPlayer()
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_play
                    )
                )
                binding.progressCircular.visibility = View.INVISIBLE
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

        viewModel.playerItemsError.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding.playerItemsError.visibility = View.VISIBLE
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_play
                    )
                )
                binding.progressCircular.visibility = View.INVISIBLE
            } else {
                binding.playerItemsError.visibility = View.GONE
            }
        })

        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment(
                viewModel.playerItemsError.value ?: getString(R.string.unknown_error)
            ).show(parentFragmentManager, "errordialog")
        }

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
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.visibility = View.VISIBLE
            if (args.itemType == "Movie") {
                if (viewModel.item.value?.mediaSources != null) {
                    if (viewModel.item.value?.mediaSources?.size!! > 1) {
                        VideoVersionDialogFragment(viewModel).show(
                            parentFragmentManager,
                            "videoversiondialog"
                        )
                    } else {
                        viewModel.preparePlayerItems()
                    }
                }
            } else if (args.itemType == "Series") {
                viewModel.preparePlayerItems()
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
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToPlayerActivity(
                playerItems,
            )
        )
    }
}