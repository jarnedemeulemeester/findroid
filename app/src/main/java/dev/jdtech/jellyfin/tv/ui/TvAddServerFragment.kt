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
import kotlinx.coroutines.flow.collect
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
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is AddServerViewModel.UiState.Normal -> {
                                binding.progressCircular.isVisible = false
                            }
                            is AddServerViewModel.UiState.Error -> {
                                binding.progressCircular.isVisible = false
                                binding.serverAddress.error = uiState.message
                            }
                            is AddServerViewModel.UiState.Loading -> {
                                binding.progressCircular.isVisible = true
                                binding.serverAddress.error = null
                            }
                        }
                    }
                }
                launch {
                    viewModel.navigateToLogin.collect {
                        Timber.d("Navigate to login: $it")
                        if (it) {
                            navigateToLoginFragment()
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(TvAddServerFragmentDirections.actionAddServerFragmentToLoginFragment())
    }
}