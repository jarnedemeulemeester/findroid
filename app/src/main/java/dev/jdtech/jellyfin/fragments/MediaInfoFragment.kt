package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
import dev.jdtech.jellyfin.utils.requestDownload
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.util.UUID

@AndroidEntryPoint
class MediaInfoFragment : Fragment() {

    private lateinit var binding: FragmentMediaInfoBinding
    private val viewModel: MediaInfoViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

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

        if(args.itemType != "Movie") {
            binding.downloadButton.visibility = View.GONE
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.itemId, args.itemType)
        }

        viewModel.downloadMedia.observe(viewLifecycleOwner, {
            if (it) {
                requestDownload(Uri.parse(viewModel.downloadRequestItem.uri), viewModel.downloadRequestItem, this)
                viewModel.doneDownloadMedia()
            }
        })

        viewModel.item.observe(viewLifecycleOwner, { item ->
            if (item.originalTitle != item.name) {
                binding.originalTitle.visibility = View.VISIBLE
            } else {
                binding.originalTitle.visibility = View.GONE
            }
            if (item.remoteTrailers.isNullOrEmpty()) {
                binding.trailerButton.visibility = View.GONE
            }
            binding.communityRating.visibility = when (item.communityRating != null) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            Timber.d(item.seasonId.toString())
        })

        viewModel.actors.observe(viewLifecycleOwner, { actors ->
            when (actors.isNullOrEmpty()) {
                false -> binding.actors.visibility = View.VISIBLE
                true -> binding.actors.visibility = View.GONE
            }
        })

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
            }
        }

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
            if (viewModel.item.value?.remoteTrailers.isNullOrEmpty()) return@setOnClickListener
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
        binding.peopleRecyclerView.adapter = PersonListAdapter { person ->
            val uuid = person.id?.toUUID()
            if (uuid != null) {
                navigateToPersonDetail(uuid)
            } else {
                Toast.makeText(requireContext(), R.string.error_getting_person_id, Toast.LENGTH_SHORT).show()
            }
        }

        binding.playButton.setOnClickListener {
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.visibility = View.VISIBLE

            viewModel.item.value?.let { item ->
                if (!args.isOffline) {
                    playerViewModel.loadPlayerItems(item) {
                        VideoVersionDialogFragment(item, playerViewModel).show(
                            parentFragmentManager,
                            "videoversiondialog"
                        )
                    }
                } else {
                    playerViewModel.loadOfflinePlayerItems(args.playerItem!!)
                }
            }
        }

        if (!args.isOffline) {
            binding.errorLayout.errorRetryButton.setOnClickListener {
                viewModel.loadData(args.itemId, args.itemType)
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

            binding.downloadButton.setOnClickListener {
                viewModel.loadDownloadRequestItem(args.itemId)
            }

            binding.deleteButton.visibility = View.GONE

            viewModel.loadData(args.itemId, args.itemType)
        } else {
            binding.favoriteButton.visibility = View.GONE
            binding.checkButton.visibility = View.GONE
            binding.downloadButton.visibility = View.GONE

            binding.deleteButton.setOnClickListener {
                viewModel.deleteItem()
                findNavController().navigate(R.id.downloadFragment)
            }

            viewModel.loadData(args.playerItem!!)
        }
    }

    private fun bindPlayerItems(items: PlayerViewModel.PlayerItems) {
        navigateToPlayerActivity(items.items.toTypedArray())
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
    }

    private fun bindPlayerItemsError(error: PlayerViewModel.PlayerItemError) {
        Timber.e(error.message)
        binding.playerItemsError.visibility = View.VISIBLE
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment(error.message).show(parentFragmentManager, "errordialog")
        }
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
                playerItems
            )
        )
    }

    private fun navigateToPersonDetail(personId: UUID) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToPersonDetailFragment(personId)
        )
    }
}