package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppNavigationDirections
import dev.jdtech.jellyfin.adapters.UserListAdapter
import dev.jdtech.jellyfin.databinding.FragmentUsersBinding
import dev.jdtech.jellyfin.dialogs.DeleteUserDialogFragment
import dev.jdtech.jellyfin.viewmodels.UsersViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private lateinit var binding: FragmentUsersBinding
    private val viewModel: UsersViewModel by viewModels()
    private val args: UsersFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentUsersBinding.inflate(inflater)

        binding.usersRecyclerView.adapter =
            UserListAdapter(
                { user ->
                    viewModel.loginAsUser(user)
                },
                { user ->
                    DeleteUserDialogFragment(viewModel, user).show(
                        parentFragmentManager,
                        "deleteUser",
                    )
                    true
                },
            )

        binding.buttonAddUser.setOnClickListener {
            navigateToLoginFragment()
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
                        is UsersViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is UsersViewModel.UiState.Loading -> Unit
                        is UsersViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        viewModel.loadUsers(args.serverId)
    }

    fun bindUiStateNormal(uiState: UsersViewModel.UiState.Normal) {
        (binding.usersRecyclerView.adapter as UserListAdapter).submitList(uiState.users)
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(
            AppNavigationDirections.actionGlobalLoginFragment(),
        )
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(UsersFragmentDirections.actionUsersFragmentToHomeFragment())
    }
}
