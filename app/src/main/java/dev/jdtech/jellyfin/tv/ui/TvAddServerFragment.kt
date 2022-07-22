package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

        binding.editTextServerAddress.setOnEditorActionListener { _, actionId, _ ->
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
        binding.buttonConnect.isEnabled = true
        binding.progressCircular.isVisible = false
    }

    private fun bindUiStateError(uiState: AddServerViewModel.UiState.Error) {
        binding.buttonConnect.isEnabled = true
        binding.progressCircular.isVisible = false
        binding.editTextServerAddress.error = uiState.message
    }

    private fun bindUiStateLoading() {
        binding.buttonConnect.isEnabled = false
        binding.progressCircular.isVisible = true
        binding.editTextServerAddress.error = null
    }

    private fun connectToServer() {
        val serverAddress = binding.editTextServerAddress.text.toString()
        viewModel.checkServer(serverAddress.removeSuffix("/"))
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(TvAddServerFragmentDirections.actionAddServerFragmentToLoginFragment())
    }
}