package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.FavoritesListAdapter
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentFavoriteBinding
import dev.jdtech.jellyfin.viewmodels.FavoriteViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private lateinit var binding: FragmentFavoriteBinding
    private val viewModel: FavoriteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        val snackbar =
            Snackbar.make(
                binding.mainLayout,
                getString(R.string.error_loading_data),
                Snackbar.LENGTH_INDEFINITE
            )
        snackbar.setAction(getString(R.string.retry)) {
            viewModel.loadData()
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.favoritesRecyclerView.adapter = FavoritesListAdapter(
            ViewItemListAdapter.OnClickListener { item ->
                navigateToMediaInfoFragment(item)
            }, HomeEpisodeListAdapter.OnClickListener { item ->
                navigateToEpisodeBottomSheetFragment(item)
            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, { isFinished ->
            binding.loadingIndicator.visibility = if (isFinished) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error) {
                snackbar.show()
            }
        })

        viewModel.favoriteSections.observe(viewLifecycleOwner, { sections ->
            if (sections.isEmpty()) {
                binding.noFavoritesText.visibility = View.VISIBLE
            } else {
                binding.noFavoritesText.visibility = View.GONE
            }
        })

        return binding.root
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        findNavController().navigate(
            FavoriteFragmentDirections.actionFavoriteFragmentToMediaInfoFragment(
                item.id,
                item.name,
                item.type ?: "Unknown"
            )
        )
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            FavoriteFragmentDirections.actionFavoriteFragmentToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }
}