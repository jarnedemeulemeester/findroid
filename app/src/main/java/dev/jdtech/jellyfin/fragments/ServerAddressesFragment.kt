package dev.jdtech.jellyfin.fragments

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.ServerAddressAdapter
import dev.jdtech.jellyfin.databinding.FragmentServerAddressesBinding
import dev.jdtech.jellyfin.dialogs.AddServerAddressDialog
import dev.jdtech.jellyfin.dialogs.DeleteServerAddressDialog
import dev.jdtech.jellyfin.viewmodels.ServerAddressesViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ServerAddressesFragment : Fragment() {

    private lateinit var binding: FragmentServerAddressesBinding
    private lateinit var uiModeManager: UiModeManager
    private val viewModel: ServerAddressesViewModel by viewModels()
    private val args: UsersFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentServerAddressesBinding.inflate(inflater)
        uiModeManager =
            requireContext().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager

        binding.addressesRecyclerView.adapter =
            ServerAddressAdapter(
                { address ->
                    viewModel.switchToAddress(address)
                },
                { address ->
                    DeleteServerAddressDialog(viewModel, address).show(
                        parentFragmentManager,
                        "deleteServerAddress"
                    )
                    true
                }
            )

        binding.buttonAddAddress.setOnClickListener {
            AddServerAddressDialog(viewModel).show(
                parentFragmentManager,
                "addServerAddress"
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToMain.collect {
                    if (it) {
                        navigateToMainActivity()
                    }
                }
            }
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
                        is ServerAddressesViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is ServerAddressesViewModel.UiState.Loading -> Unit
                        is ServerAddressesViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        viewModel.loadAddresses(args.serverId)
    }

    fun bindUiStateNormal(uiState: ServerAddressesViewModel.UiState.Normal) {
        (binding.addressesRecyclerView.adapter as ServerAddressAdapter).submitList(uiState.addresses)
    }

    private fun navigateToMainActivity() {
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            findNavController().navigate(UsersFragmentDirections.actionUsersFragmentToHomeFragmentTv())
        } else {
            findNavController().navigate(UsersFragmentDirections.actionUsersFragmentToHomeFragment())
        }
    }
}
