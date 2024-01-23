package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.DiscoveredServerListAdapter
import dev.jdtech.jellyfin.databinding.FragmentAddServerBinding
import dev.jdtech.jellyfin.viewmodels.AddServerEvent
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AddServerFragment : Fragment() {

    private lateinit var binding: FragmentAddServerBinding
    private val viewModel: AddServerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddServerBinding.inflate(inflater)

        binding.privacyPolicyText.movementMethod = LinkMovementMethod.getInstance()

        (binding.editTextServerAddress as AppCompatEditText).setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    connectToServer()
                    true
                }
                else -> false
            }
        }

        binding.buttonConnect.setOnClickListener {
            connectToServer()
        }

        binding.serversRecyclerView.adapter = DiscoveredServerListAdapter { server ->
            (binding.editTextServerAddress as AppCompatEditText).setText(server.address)
            connectToServer()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is AddServerViewModel.UiState.Normal -> bindUiStateNormal()
                        is AddServerViewModel.UiState.Error -> bindUiStateError(uiState)
                        is AddServerViewModel.UiState.Loading -> bindUiStateLoading()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.discoveredServersState.collect { serversState ->
                    when (serversState) {
                        is AddServerViewModel.DiscoveredServersState.Loading -> Unit
                        is AddServerViewModel.DiscoveredServersState.Servers -> bindDiscoveredServersStateServers(serversState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsChannelFlow.collect { event ->
                    when (event) {
                        is AddServerEvent.NavigateToLogin -> navigateToLoginFragment()
                    }
                }
            }
        }

        return binding.root
    }

    private fun bindUiStateNormal() {
        binding.buttonConnect.isEnabled = true
        binding.progressCircular.isVisible = false
        binding.editTextServerAddressLayout.isEnabled = true
    }

    private fun bindUiStateError(uiState: AddServerViewModel.UiState.Error) {
        binding.buttonConnect.isEnabled = true
        binding.progressCircular.isVisible = false
        binding.editTextServerAddressLayout.apply {
            error = uiState.message.joinToString { it.asString(resources) }
            isEnabled = true
        }
    }

    private fun bindUiStateLoading() {
        binding.buttonConnect.isEnabled = false
        binding.progressCircular.isVisible = true
        binding.editTextServerAddressLayout.apply {
            error = null
            isEnabled = false
        }
    }

    private fun bindDiscoveredServersStateServers(
        serversState: AddServerViewModel.DiscoveredServersState.Servers,
    ) {
        val servers = serversState.servers
        if (servers.isEmpty()) {
            binding.serversRecyclerView.isVisible = false
        } else {
            binding.serversRecyclerView.isVisible = true
            (binding.serversRecyclerView.adapter as DiscoveredServerListAdapter).submitList(servers)
        }
    }

    private fun connectToServer() {
        val serverAddress = (binding.editTextServerAddress as AppCompatEditText).text.toString()
        viewModel.checkServer(serverAddress.removeSuffix("/"))
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(AddServerFragmentDirections.actionAddServerFragmentToLoginFragment())
    }
}
