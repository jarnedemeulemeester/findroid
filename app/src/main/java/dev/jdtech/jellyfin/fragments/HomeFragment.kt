package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        findNavController().graph.startDestination = R.id.homeFragment
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navigateToSettingsFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val snackbar =
            Snackbar.make(binding.mainLayout, getString(R.string.error_loading_data), Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(getString(R.string.retry)) {
            viewModel.loadData()
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter = ViewListAdapter(ViewListAdapter.OnClickListener {
            navigateToLibraryFragment(it)
        }, ViewItemListAdapter.OnClickListener {
            navigateToMediaInfoFragment(it)
        }, HomeEpisodeListAdapter.OnClickListener { item ->
            when (item.type) {
                "Episode" -> {
                    navigateToEpisodeBottomSheetFragment(item)
                }
                "Movie" -> {
                    navigateToMediaInfoFragment(item)
                }
            }

        })

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error) {
                snackbar.show()
            }
        })

        return binding.root
    }

    private fun navigateToLibraryFragment(view: dev.jdtech.jellyfin.models.View) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                view.id,
                view.name
            )
        )
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        if (item.type == "Episode") {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    item.seriesId!!,
                    item.seriesName,
                    "Series"
                )
            )
        } else {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    item.id,
                    item.name,
                    item.type ?: "Unknown"
                )
            )
        }
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToNavigationSettings()
        )
    }
}