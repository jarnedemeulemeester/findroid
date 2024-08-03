package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.FragmentPlayerGesturesBinding
import dev.jdtech.jellyfin.viewmodels.GesturesViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class PlayerGesturesFragment : Fragment() {

    private lateinit var binding: FragmentPlayerGesturesBinding
    private val viewModel: GesturesViewModel by viewModels()
    private val args: PlayerGesturesFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPlayerGesturesBinding.inflate(inflater)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(CoreR.menu.gestures_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        CoreR.id.action_change_profile -> {
                            // TODO: Load profile
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        // binding.gesturesRecyclerView.adapter =
            // TODO: Add gesture adapter

        binding.buttonAddGesture.setOnClickListener {
            // TODO: Navigate to new gesture fragment / dialog
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is GesturesViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is GesturesViewModel.UiState.Loading -> Unit
                        is GesturesViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        viewModel.loadGestures(args.profileId)
    }

    fun bindUiStateNormal(uiState: GesturesViewModel.UiState.Normal) {
        // TODO: load player gestures
    }
}
