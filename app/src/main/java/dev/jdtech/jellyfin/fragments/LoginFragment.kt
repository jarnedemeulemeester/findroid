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
import dev.jdtech.jellyfin.adapters.UserListAdapter
import dev.jdtech.jellyfin.databinding.FragmentLoginBinding
import dev.jdtech.jellyfin.viewmodels.LoginViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var uiModeManager: UiModeManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        uiModeManager =
            requireContext().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager

        (binding.editTextPassword as AppCompatEditText).setOnEditorActionListener { _, actionId, _ ->
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

        binding.usersRecyclerView.adapter = UserListAdapter { user ->
            (binding.editTextUsername as AppCompatEditText).setText(user.name)
            (binding.editTextPassword as AppCompatEditText).requestFocus()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is LoginViewModel.UiState.Normal -> bindUiStateNormal()
                        is LoginViewModel.UiState.Error -> bindUiStateError(uiState)
                        is LoginViewModel.UiState.Loading -> bindUiStateLoading()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.usersState.collect { usersState ->
                    when (usersState) {
                        is LoginViewModel.UsersState.Loading -> Unit
                        is LoginViewModel.UsersState.Users -> bindUsersStateUsers(usersState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToMain.collect {
                    if (it) {
                        navigateToHomeFragment()
                    }
                }
            }
        }

        return binding.root
    }

    private fun bindUiStateNormal() {
        binding.buttonLogin.isEnabled = true
        binding.progressCircular.isVisible = false
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            (binding.editTextUsername as AppCompatEditText).isEnabled = true
            (binding.editTextPassword as AppCompatEditText).isEnabled = true
        } else {
            binding.editTextUsernameLayout!!.isEnabled = true
            binding.editTextPasswordLayout!!.isEnabled = true
        }
    }

    private fun bindUiStateError(uiState: LoginViewModel.UiState.Error) {
        binding.buttonLogin.isEnabled = true
        binding.progressCircular.isVisible = false
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            (binding.editTextUsername as AppCompatEditText).apply {
                error = uiState.message
                isEnabled = true
            }
            (binding.editTextPassword as AppCompatEditText).isEnabled = true
        } else {
            binding.editTextUsernameLayout!!.apply {
                error = uiState.message
                isEnabled = true
            }
            binding.editTextPasswordLayout!!.isEnabled = true
        }
    }

    private fun bindUiStateLoading() {
        binding.buttonLogin.isEnabled = false
        binding.progressCircular.isVisible = true
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            (binding.editTextUsername as AppCompatEditText).apply {
                error = null
                isEnabled = false
            }
            (binding.editTextPassword as AppCompatEditText).isEnabled = false
        } else {
            binding.editTextUsernameLayout!!.apply {
                error = null
                isEnabled = false
            }
            binding.editTextPasswordLayout!!.isEnabled = false
        }
    }

    private fun bindUsersStateUsers(usersState: LoginViewModel.UsersState.Users) {
        val users = usersState.users
        if (users.isEmpty()) {
            binding.usersRecyclerView.isVisible = false
        } else {
            binding.usersRecyclerView.isVisible = true
            (binding.usersRecyclerView.adapter as UserListAdapter).submitList(users)
        }
    }

    private fun login() {
        val username = (binding.editTextUsername as AppCompatEditText).text.toString()
        val password = (binding.editTextPassword as AppCompatEditText).text.toString()
        viewModel.login(username, password)
    }

    private fun navigateToHomeFragment() {
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToHomeFragmentTv())
        } else {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToHomeFragment())
        }
    }
}
