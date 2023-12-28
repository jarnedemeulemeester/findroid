package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.databinding.FragmentServerSelectBinding
import dev.jdtech.jellyfin.dialogs.DeleteServerDialogFragment
import dev.jdtech.jellyfin.viewmodels.ServerSelectEvent
import dev.jdtech.jellyfin.viewmodels.ServerSelectViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ServerSelectFragment : Fragment() {
    private lateinit var binding: FragmentServerSelectBinding
    private val viewModel: ServerSelectViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentServerSelectBinding.inflate(inflater)

        binding.serversRecyclerView.adapter =
            ServerGridAdapter(
                onClickListener = { server ->
                    viewModel.connectToServer(server)
                },
                onLongClickListener = { server ->
                    DeleteServerDialogFragment(viewModel, server).show(
                        parentFragmentManager,
                        "deleteServer",
                    )
                    true
                },
            )

        binding.buttonAddServer.setOnClickListener {
            navigateToAddServerFragment()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is ServerSelectViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                            is ServerSelectViewModel.UiState.Loading -> Unit
                            is ServerSelectViewModel.UiState.Error -> Unit
                        }
                    }
                }
                launch {
                    viewModel.eventsChannelFlow.collect { event ->
                        when (event) {
                            is ServerSelectEvent.NavigateToHome -> navigateToMainActivity()
                            is ServerSelectEvent.NavigateToLogin -> navigateToLoginFragment()
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun bindUiStateNormal(uiState: ServerSelectViewModel.UiState.Normal) {
        uiState.apply {
            (binding.serversRecyclerView.adapter as ServerGridAdapter).submitList(servers)
        }
    }

    private fun navigateToAddServerFragment() {
        findNavController().navigate(
            ServerSelectFragmentDirections.actionServerSelectFragmentToAddServerFragment(),
        )
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(ServerSelectFragmentDirections.actionServerSelectFragmentToHomeFragment())
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(ServerSelectFragmentDirections.actionServerSelectFragmentToLoginFragment())
    }
}
