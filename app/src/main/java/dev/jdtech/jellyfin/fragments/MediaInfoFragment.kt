package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.bindBaseItemImage
import dev.jdtech.jellyfin.bindItemBackdropImage
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
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

    lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaInfoBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is MediaInfoViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is MediaInfoViewModel.UiState.Loading -> bindUiStateLoading()
                        is MediaInfoViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
                if (!args.isOffline) {
                    viewModel.loadData(args.itemId, args.itemType)
                } else {
                    viewModel.loadData(args.playerItem!!)
                }
            }
        }

        if (args.itemType != "Movie") {
            binding.downloadButton.visibility = View.GONE
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.itemId, args.itemType)
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
            }
        }

        binding.trailerButton.setOnClickListener {
            if (viewModel.item?.remoteTrailers.isNullOrEmpty()) return@setOnClickListener
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(viewModel.item?.remoteTrailers?.get(0)?.url)
            )
            startActivity(intent)
        }

        binding.nextUp.setOnClickListener {
            navigateToEpisodeBottomSheetFragment(viewModel.nextUp!!)
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
                Toast.makeText(
                    requireContext(),
                    R.string.error_getting_person_id,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.playButton.setOnClickListener {
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.isVisible = true
            viewModel.item?.let { item ->
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
                when (viewModel.played) {
                    true -> {
                        viewModel.markAsUnplayed(args.itemId)
                        val typedValue = TypedValue()
                        requireActivity().theme.resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true)
                        binding.checkButton.imageTintList = ColorStateList.valueOf(
                            resources.getColor(
                                typedValue.resourceId,
                                requireActivity().theme
                            )
                        )
                    }
                    false -> {
                        viewModel.markAsPlayed(args.itemId)
                        binding.checkButton.imageTintList = ColorStateList.valueOf(
                            resources.getColor(
                                R.color.red,
                                requireActivity().theme
                            )
                        )
                    }
                }
            }

            binding.favoriteButton.setOnClickListener {
                when (viewModel.favorite) {
                    true -> {
                        viewModel.unmarkAsFavorite(args.itemId)
                        binding.favoriteButton.setImageResource(R.drawable.ic_heart)
                        val typedValue = TypedValue()
                        requireActivity().theme.resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true)
                        binding.favoriteButton.imageTintList = ColorStateList.valueOf(
                            resources.getColor(
                                typedValue.resourceId,
                                requireActivity().theme
                            )
                        )
                    }
                    false -> {
                        viewModel.markAsFavorite(args.itemId)
                        binding.favoriteButton.setImageResource(R.drawable.ic_heart_filled)
                        binding.favoriteButton.imageTintList = ColorStateList.valueOf(
                            resources.getColor(
                                R.color.red,
                                requireActivity().theme
                            )
                        )
                    }
                }
            }

            binding.downloadButton.setOnClickListener {
                binding.downloadButton.isEnabled = false
                viewModel.loadDownloadRequestItem(args.itemId)
                binding.downloadButton.imageTintList = ColorStateList.valueOf(
                    resources.getColor(
                        R.color.red,
                        requireActivity().theme
                    )
                )
            }

        } else {
            binding.favoriteButton.isVisible = false
            binding.checkButton.isVisible = false
            binding.downloadButton.isVisible = false
            binding.deleteButton.isVisible = true

            binding.deleteButton.setOnClickListener {
                viewModel.deleteItem()
                findNavController().navigate(R.id.downloadFragment)
            }
        }
    }

    private fun bindUiStateNormal(uiState: MediaInfoViewModel.UiState.Normal) {
        uiState.apply {
            binding.originalTitle.isVisible = item.originalTitle != item.name
            if (item.remoteTrailers.isNullOrEmpty()) {
                binding.trailerButton.isVisible = false
            }
            binding.communityRating.isVisible = item.communityRating != null
            binding.actors.isVisible = actors.isNotEmpty()

            binding.playButton.isEnabled = available
            binding.playButton.alpha = if (!available) 0.5F else 1.0F

            // Check icon
            when (played) {
                true -> {
                    if (played) binding.checkButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            R.color.red,
                            requireActivity().theme
                        )
                    )
                }
                false -> {
                    val typedValue = TypedValue()
                    requireActivity().theme.resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true)
                    binding.checkButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            typedValue.resourceId,
                            requireActivity().theme
                        )
                    )
                }
            }


            // Favorite icon
            val favoriteDrawable = when (favorite) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }
            binding.favoriteButton.setImageResource(favoriteDrawable)
            if (favorite) binding.favoriteButton.imageTintList = ColorStateList.valueOf(
                resources.getColor(
                    R.color.red,
                    requireActivity().theme
                )
            )

            binding.downloadButton.isEnabled = !downloaded

            when (canDownload) {
                true -> {
                    binding.downloadButton.isVisible = true
                    binding.downloadButton.isEnabled = !downloaded

                    if (downloaded) binding.downloadButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            R.color.red,
                            requireActivity().theme
                        )
                    )
                }
                false -> {
                    binding.downloadButton.isVisible = false
                }
            }


            binding.name.text = item.name
            binding.originalTitle.text = item.originalTitle
            if (dateString.isEmpty()) {
                binding.year.isVisible = false
            } else {
                binding.year.text = dateString
            }
            if (runTime.isEmpty()) {
                binding.playtime.isVisible = false
            } else {
                binding.playtime.text = runTime
            }
            binding.officialRating.text = item.officialRating
            binding.communityRating.text = item.communityRating.toString()
            binding.genresLayout.isVisible = item.genres?.isNotEmpty() ?: false
            binding.genres.text = genresString
            binding.directorLayout.isVisible = director != null
            binding.director.text = director?.name
            binding.writersLayout.isVisible = writers.isNotEmpty()
            binding.writers.text = writersString
            binding.description.text = item.overview
            binding.nextUpLayout.isVisible = nextUp != null
            binding.nextUpName.text = String.format(
                getString(R.string.episode_name_extended),
                nextUp?.parentIndexNumber,
                nextUp?.indexNumber,
                nextUp?.name
            )
            binding.seasonsLayout.isVisible = seasons.isNotEmpty()
            val seasonsAdapter = binding.seasonsRecyclerView.adapter as ViewItemListAdapter
            seasonsAdapter.submitList(seasons)
            val actorsAdapter = binding.peopleRecyclerView.adapter as PersonListAdapter
            actorsAdapter.submitList(actors)
            bindItemBackdropImage(binding.itemBanner, item)
            bindBaseItemImage(binding.nextUpImage, nextUp)
        }
        binding.loadingIndicator.isVisible = false
        binding.mediaInfoScrollview.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: MediaInfoViewModel.UiState.Error) {
        val error = uiState.message ?: getString(R.string.unknown_error)
        binding.loadingIndicator.isVisible = false
        binding.mediaInfoScrollview.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(error)
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