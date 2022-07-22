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
import dev.jdtech.jellyfin.databinding.FragmentLoginBinding
import dev.jdtech.jellyfin.viewmodels.LoginViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)

        binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    login()
                    true
                }
                else -> false
            }
        }

        binding.buttonLogin.setOnClickListener {
            login()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when(uiState) {
                        is LoginViewModel.UiState.Normal -> bindUiStateNormal()
                        is LoginViewModel.UiState.Error -> bindUiStateError(uiState)
                        is LoginViewModel.UiState.Loading -> bindUiStateLoading()
                    }
                }
            }
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

    private fun bindUiStateNormal() {
        binding.buttonLogin.isEnabled = true
        binding.progressCircular.isVisible = false
    }

    private fun bindUiStateError(uiState: LoginViewModel.UiState.Error) {
        binding.buttonLogin.isEnabled = true
        binding.progressCircular.isVisible = false
        binding.editTextUsernameLayout.error = uiState.message
    }

    private fun bindUiStateLoading() {
        binding.buttonLogin.isEnabled = false
        binding.progressCircular.isVisible = true
        binding.editTextUsernameLayout.error = null
    }

    private fun login() {
        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()
        viewModel.login(username, password)
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToNavigationHome())
    }
}