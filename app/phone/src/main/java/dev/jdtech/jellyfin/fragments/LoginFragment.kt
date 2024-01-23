package dev.jdtech.jellyfin.fragments

import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.adapters.UserLoginListAdapter
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.databinding.FragmentLoginBinding
import dev.jdtech.jellyfin.viewmodels.LoginEvent
import dev.jdtech.jellyfin.viewmodels.LoginViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val args: LoginFragmentArgs by navArgs()

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var dataBase: ServerDatabaseDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)

        if (args.reLogin) {
            appPreferences.currentServer?.let { currentServerId ->
                dataBase.getServerCurrentUser(currentServerId)?.let { user ->
                    (binding.editTextUsername as AppCompatEditText).setText(user.name)
                }
            }
        }

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

        binding.buttonQuickconnect.setOnClickListener {
            viewModel.useQuickConnect()
        }

        binding.usersRecyclerView.adapter = UserLoginListAdapter { user ->
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
                viewModel.quickConnectUiState.collect { quickConnectUiState ->
                    when (quickConnectUiState) {
                        is LoginViewModel.QuickConnectUiState.Disabled -> {
                            binding.buttonQuickconnectLayout.isVisible = false
                        }
                        is LoginViewModel.QuickConnectUiState.Normal -> {
                            binding.buttonQuickconnectLayout.isVisible = true
                            binding.buttonQuickconnect.text = resources.getString(CoreR.string.quick_connect)
                            binding.buttonQuickconnectProgress.isVisible = false
                        }
                        is LoginViewModel.QuickConnectUiState.Waiting -> {
                            binding.buttonQuickconnect.text = quickConnectUiState.code
                            binding.buttonQuickconnectProgress.isVisible = true
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsChannelFlow.collect { event ->
                    when (event) {
                        is LoginEvent.NavigateToHome -> navigateToHomeFragment()
                    }
                }
            }
        }

        return binding.root
    }

    private fun bindUiStateNormal() {
        binding.buttonLogin.isEnabled = true
        binding.progressCircular.isVisible = false
        binding.editTextUsernameLayout.isEnabled = true
        binding.editTextPasswordLayout.isEnabled = true
    }

    private fun bindUiStateError(uiState: LoginViewModel.UiState.Error) {
        binding.buttonLogin.isEnabled = true
        binding.progressCircular.isVisible = false
        binding.editTextUsernameLayout.apply {
            error = uiState.message.asString(resources)
            isEnabled = true
        }
        binding.editTextPasswordLayout.isEnabled = true
    }

    private fun bindUiStateLoading() {
        binding.buttonLogin.isEnabled = false
        binding.progressCircular.isVisible = true
        binding.editTextUsernameLayout.apply {
            error = null
            isEnabled = false
        }
        binding.editTextPasswordLayout.isEnabled = false
    }

    private fun bindUsersStateUsers(usersState: LoginViewModel.UsersState.Users) {
        val users = usersState.users
        if (users.isEmpty()) {
            binding.usersRecyclerView.isVisible = false
        } else {
            binding.usersRecyclerView.isVisible = true
            (binding.usersRecyclerView.adapter as UserLoginListAdapter).submitList(users)
        }
    }

    private fun login() {
        val username = (binding.editTextUsername as AppCompatEditText).text.toString()
        val password = (binding.editTextPassword as AppCompatEditText).text.toString()
        viewModel.login(username, password)
    }

    private fun navigateToHomeFragment() {
        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToHomeFragment())
    }
}
