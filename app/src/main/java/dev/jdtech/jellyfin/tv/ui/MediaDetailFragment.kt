package dev.jdtech.jellyfin.tv.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.MediaDetailFragmentBinding
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.tv.ui.MediaDetailViewModel.State.Movie
import dev.jdtech.jellyfin.tv.ui.MediaDetailViewModel.State.TvShow
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel.PlayerItemError
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel.PlayerItems
import timber.log.Timber

@AndroidEntryPoint
internal class MediaDetailFragment : Fragment() {

    private lateinit var binding: MediaDetailFragmentBinding

    private val viewModel: MediaInfoViewModel by viewModels()
    private val detailViewModel: MediaDetailViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private val args: MediaDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadData(args.itemId, args.itemType)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MediaDetailFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.item = detailViewModel.transformData(viewModel.item, resources) {
            bindActions(it)
            bindState(it)
        }

        val seasonsAdapter = ViewItemListAdapter(
            fixedWidth = true,
            onClickListener = ViewItemListAdapter.OnClickListener {})

        viewModel.seasons.observe(viewLifecycleOwner) {
            seasonsAdapter.submitList(it)
            binding.seasonTitle.isVisible = true
        }

        binding.seasonsRow.gridView.adapter = seasonsAdapter
        binding.seasonsRow.gridView.verticalSpacing = 25

        val castAdapter = PersonListAdapter { person ->
            Toast.makeText(requireContext(), "Not yet implemented", Toast.LENGTH_SHORT).show()
        }

        viewModel.actors.observe(viewLifecycleOwner) { cast ->
            castAdapter.submitList(cast)
            binding.castTitle.isVisible = cast.isNotEmpty()
        }

        binding.castRow.gridView.adapter = castAdapter
        binding.castRow.gridView.verticalSpacing = 25
    }

    private fun bindState(state: MediaDetailViewModel.State) {
        playerViewModel.onPlaybackRequested(lifecycleScope) { state ->
            when (state) {
                is PlayerItemError -> bindPlayerItemsError(state)
                is PlayerItems -> bindPlayerItems(state)
            }
        }

        when (state.media) {
            is Movie -> binding.title.text = state.media.title
            is TvShow -> with(binding.subtitle) {
                binding.title.text = state.media.episode
                text = state.media.show
                isVisible = true
            }
        }
    }

    private fun bindPlayerItems(items: PlayerItems) {
        navigateToPlayerActivity(items.items.toTypedArray())
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
    }

    private fun bindPlayerItemsError(error: PlayerItemError) {
        Timber.e(error.message)

        binding.errorLayout.errorPanel.isVisible = true
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
    }

    private fun bindActions(state: MediaDetailViewModel.State) {
        binding.playButton.setOnClickListener {
            binding.progressCircular.isVisible = true
            viewModel.item.value?.let { item ->
                playerViewModel.loadPlayerItems(item) {
                    VideoVersionDialogFragment(item, playerViewModel).show(
                        parentFragmentManager,
                        "videoversiondialog"
                    )
                }
            }
        }

        if (state.trailerUrl != null) {
            with(binding.trailerButton) {
                isVisible = true
                setOnClickListener { playTrailer(state.trailerUrl) }
            }
        } else {
            binding.trailerButton.isVisible = false
        }

        if (state.isPlayed) {
            with(binding.checkButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_check_filled))
                setOnClickListener { viewModel.markAsUnplayed(args.itemId) }
            }
        } else {
            with(binding.checkButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_check))
                setOnClickListener { viewModel.markAsPlayed(args.itemId) }
            }
        }

        if (state.isFavorite) {
            with(binding.favoriteButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_heart_filled))
                setOnClickListener { viewModel.unmarkAsFavorite(args.itemId) }
            }
        } else {
            with(binding.favoriteButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_heart))
                setOnClickListener { viewModel.markAsFavorite(args.itemId) }
            }
        }

        binding.backButton.setOnClickListener { activity?.onBackPressed() }
    }

    private fun playTrailer(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            MediaDetailFragmentDirections.actionMediaDetailFragmentToPlayerActivity(
                playerItems
            )
        )
    }
}