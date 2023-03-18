package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.StorageListAdapter
import dev.jdtech.jellyfin.databinding.ActivityStorageBinding
import dev.jdtech.jellyfin.viewmodels.StorageViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StorageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStorageBinding
    private val viewModel: StorageViewModel by viewModels()

    private var serverIds = emptyList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.itemsRecyclerView.adapter = StorageListAdapter()

        (binding.serversDropDown.editText as MaterialAutoCompleteTextView).setOnItemClickListener { _, _, position, _ ->
            serverIds.getOrNull(position)?.let { id ->
                viewModel.loadItems(id)
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.serversState.collect { servers ->
                    serverIds = servers.map { it.id }
                    (binding.serversDropDown.editText as MaterialAutoCompleteTextView).setSimpleItems(
                        servers.map { it.name }.toTypedArray()
                    )
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.itemsState.collect { items ->
                    (binding.itemsRecyclerView.adapter as StorageListAdapter).submitList(items)
                }
            }
        }
    }
}
