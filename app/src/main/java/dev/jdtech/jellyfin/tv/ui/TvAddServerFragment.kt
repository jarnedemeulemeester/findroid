package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.TvAddServerFragmentBinding
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
internal class TvAddServerFragment : Fragment() {

    private lateinit var binding: TvAddServerFragmentBinding
    private val viewModel: AddServerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TvAddServerFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.buttonConnect.setOnClickListener {
            val serverAddress = binding.serverAddress.text.toString()
            viewModel.checkServer(serverAddress)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is AddServerViewModel.UiState.Normal -> bindUiStateNormal()
                        is AddServerViewModel.UiState.Error -> bindUiStateError(uiState)
                        is AddServerViewModel.UiState.Loading -> bindUiStateLoading()
                    }
                }
                viewModel.onNavigateToLogin(viewLifecycleOwner.lifecycleScope) {
                    Timber.d("Navigate to login: $it")
                    if (it) {
                        navigateToLoginFragment()
                    }
                }
            }
        }

        return binding.root
    }

    private fun bindUiStateNormal() {
        binding.progressCircular.isVisible = false
    }

    private fun bindUiStateError(uiState: AddServerViewModel.UiState.Error) {
        binding.progressCircular.isVisible = false
        binding.serverAddress.error = uiState.message
    }

    private fun bindUiStateLoading() {
        binding.progressCircular.isVisible = true
        binding.serverAddress.error = null
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(TvAddServerFragmentDirections.actionAddServerFragmentToLoginFragment())
    }
}