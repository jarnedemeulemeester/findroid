package dev.jdtech.jellyfin.fragments

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
import dev.jdtech.jellyfin.databinding.FragmentAddServerBinding
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AddServerFragment : Fragment() {

    private lateinit var binding: FragmentAddServerBinding
    private val viewModel: AddServerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddServerBinding.inflate(inflater)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

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
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is AddServerViewModel.UiState.Normal -> {
                                binding.progressCircular.isVisible = false
                            }
                            is AddServerViewModel.UiState.Error -> {
                                binding.progressCircular.isVisible = false
                                binding.editTextServerAddressLayout.error = uiState.message
                            }
                            is AddServerViewModel.UiState.Loading -> {
                                binding.progressCircular.isVisible = true
                                binding.editTextServerAddressLayout.error = null
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

    private fun connectToServer() {
        val serverAddress = binding.editTextServerAddress.text.toString()
        viewModel.checkServer(serverAddress)
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(AddServerFragmentDirections.actionAddServerFragment3ToLoginFragment2())
    }
}