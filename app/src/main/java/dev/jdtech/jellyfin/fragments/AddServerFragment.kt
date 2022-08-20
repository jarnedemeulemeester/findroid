package dev.jdtech.jellyfin.fragments

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
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
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AddServerFragment : Fragment() {

    private lateinit var binding: FragmentAddServerBinding
    private lateinit var uiModeManager: UiModeManager
    private val viewModel: AddServerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddServerBinding.inflate(inflater)
        uiModeManager =
            requireContext().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager

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
                viewModel.navigateToLogin.collect {
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
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            (binding.editTextServerAddress as AppCompatEditText).error = uiState.message
        } else {
            binding.editTextServerAddressLayout!!.error = uiState.message
        }
    }

    private fun bindUiStateLoading() {
        binding.buttonConnect.isEnabled = false
        binding.progressCircular.isVisible = true
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            (binding.editTextServerAddress as AppCompatEditText).error = null
        } else {
            binding.editTextServerAddressLayout!!.error = null
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