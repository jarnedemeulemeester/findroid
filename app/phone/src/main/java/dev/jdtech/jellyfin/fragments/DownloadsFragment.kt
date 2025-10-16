package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.DownloadsAdapter
import dev.jdtech.jellyfin.databinding.FragmentDownloadsBinding
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.restart
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.viewmodels.DownloadsEvent
import dev.jdtech.jellyfin.viewmodels.DownloadsViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class DownloadsFragment : Fragment() {
    private lateinit var binding: FragmentDownloadsBinding
    private val viewModel: DownloadsViewModel by activityViewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var downloader: Downloader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        android.util.Log.d("DownloadsUI", "======== onCreateView ========")
        binding = FragmentDownloadsBinding.inflate(inflater, container, false)

        // Configure GridLayoutManager explicitly
        binding.downloadsRecyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(
            requireContext(),
            3 // 3 columns
        )
        
        binding.downloadsRecyclerView.adapter = DownloadsAdapter(
            onItemClickListener = { item -> navigateToMediaItem(item) },
            onItemLongClickListener = { item -> onItemLongClick(item) },
        )
        binding.downloadsRecyclerView.setHasFixedSize(true)
        
        android.util.Log.d("DownloadsUI", "RecyclerView configured with GridLayoutManager (3 columns)")
        android.util.Log.d("DownloadsUI", "Fragment instance hashCode=${this.hashCode()}, ViewModel hashCode=${viewModel.hashCode()}")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventsChannelFlow.collect { event ->
                        when (event) {
                            is DownloadsEvent.ConnectionError -> {
                                Snackbar.make(binding.root, CoreR.string.no_server_connection, Snackbar.LENGTH_INDEFINITE)
                                    .setTextMaxLines(2)
                                    .setAction(CoreR.string.offline_mode) {
                                        appPreferences.setValue(appPreferences.offlineMode, true)
                                        activity?.restart()
                                    }
                                    .show()
                            }
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("Downloads state: $uiState")
                        when (uiState) {
                            is DownloadsViewModel.UiState.Normal -> {
                                bindUiStateNormal(uiState)
                            }
                            is DownloadsViewModel.UiState.Loading -> {
                                bindUiStateLoading()
                            }
                            is DownloadsViewModel.UiState.Error -> {
                                bindUiStateError(uiState)
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("DownloadsUI", "======== onResume ======== Fragment is VISIBLE")
        android.util.Log.d("DownloadsUI", "Root view visibility: ${binding.root.visibility}, isShown: ${binding.root.isShown}")
        android.util.Log.d("DownloadsUI", "Fragment: isAdded=${isAdded}, isVisible=${isVisible}, isResumed=${isResumed}, isHidden=${isHidden}")
        
        // Log activity fragments
        activity?.supportFragmentManager?.fragments?.let { fragments ->
            android.util.Log.d("DownloadsUI", "Activity has ${fragments.size} fragments:")
            fragments.forEach { frag ->
                android.util.Log.d("DownloadsUI", "  - ${frag.javaClass.simpleName}: isVisible=${frag.isVisible}, isHidden=${frag.isHidden}, isAdded=${frag.isAdded}")
            }
        }
        
        // Force view visibility when returning from navigation
        binding.root.post {
            binding.root.visibility = android.view.View.VISIBLE
            binding.root.bringToFront()
            binding.root.requestLayout()
            binding.root.invalidate()
            
            // CRITICAL: Force RecyclerView visible immediately
            binding.downloadsRecyclerView.visibility = android.view.View.VISIBLE
            binding.downloadsRecyclerView.alpha = 1f
            binding.downloadsRecyclerView.bringToFront()
            
            // Hide all overlays
            binding.loadingIndicator.visibility = android.view.View.GONE
            binding.errorLayout.errorPanel.visibility = android.view.View.GONE
            binding.noDownloadsText.visibility = android.view.View.GONE
            
            // Force complete remeasure and relayout
            binding.root.requestLayout()
            binding.downloadsRecyclerView.requestLayout()
            
            // Post another task to verify size after layout
            binding.root.post {
                android.util.Log.d("DownloadsUI", "RecyclerView size after double post: ${binding.downloadsRecyclerView.width}x${binding.downloadsRecyclerView.height}")
                android.util.Log.d("DownloadsUI", "Root alpha: ${binding.root.alpha}, RecyclerView alpha: ${binding.downloadsRecyclerView.alpha}")
                android.util.Log.d("DownloadsUI", "RecyclerView background: ${binding.downloadsRecyclerView.background}")
                android.util.Log.d("DownloadsUI", "RecyclerView childCount: ${binding.downloadsRecyclerView.childCount}")
                
                // Check each child
                for (i in 0 until binding.downloadsRecyclerView.childCount) {
                    val child = binding.downloadsRecyclerView.getChildAt(i)
                    android.util.Log.d("DownloadsUI", "  Item $i: visible=${child.visibility}, alpha=${child.alpha}, width=${child.width}, height=${child.height}, background=${child.background}")
                }
                
                // If still wrong size, force layout params
                if (binding.downloadsRecyclerView.width < 500) {
                    android.util.Log.d("DownloadsUI", "RecyclerView too narrow! Forcing layout params")
                    binding.downloadsRecyclerView.layoutParams = binding.downloadsRecyclerView.layoutParams.apply {
                        width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    binding.downloadsRecyclerView.requestLayout()
                    
                    binding.root.post {
                        android.util.Log.d("DownloadsUI", "RecyclerView size after forced params: ${binding.downloadsRecyclerView.width}x${binding.downloadsRecyclerView.height}")
                    }
                }
            }
            
            val parent = binding.root.parent as? android.view.ViewGroup
            parent?.let {
                it.invalidate()
                android.util.Log.d("DownloadsUI", "Parent ViewGroup has ${it.childCount} children")
                for (i in 0 until it.childCount) {
                    val child = it.getChildAt(i)
                    android.util.Log.d("DownloadsUI", "  Child $i: ${child.javaClass.simpleName}, visibility=${child.visibility}, z=${child.z}, isShown=${child.isShown}")
                }
            }
            
            android.util.Log.d("DownloadsUI", "Forced root view to front, isShown now: ${binding.root.isShown}, z=${binding.root.z}")
            android.util.Log.d("DownloadsUI", "RecyclerView AFTER forcing: visibility=${binding.downloadsRecyclerView.visibility}, isShown=${binding.downloadsRecyclerView.isShown}, alpha=${binding.downloadsRecyclerView.alpha}")
        }
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("DownloadsUI", "======== onPause ======== Fragment going HIDDEN")
    }

    private fun onItemLongClick(item: FindroidItem) {
        // Only act on items that have a local source
        val localSource = item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
        if (localSource == null) return

        val isDownloading = item.isDownloading()
        val title = if (isDownloading) CoreR.string.cancel_download else CoreR.string.remove
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setNegativeButton(CoreR.string.cancel, null)
            .setPositiveButton(title) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (isDownloading) {
                            downloader.cancelDownload(item, localSource)
                        } else {
                            downloader.deleteItem(item, localSource)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to ${if (isDownloading) "cancel" else "remove"} download")
                    } finally {
                        viewModel.loadData()
                    }
                }
            }
            .show()
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)
        // Save RecyclerView state
        binding.downloadsRecyclerView.layoutManager?.onSaveInstanceState()?.let {
            outState.putParcelable("recycler_state", it)
            android.util.Log.d("DownloadsUI", "Saved RecyclerView state")
        }
    }

    private fun bindUiStateNormal(uiState: DownloadsViewModel.UiState.Normal) {
        Timber.d("Downloads: ${uiState.items.size} items")
        android.util.Log.d("DownloadsUI", "bindUiStateNormal: ${uiState.items.size} items")
        
        // Log first item details for debugging
        if (uiState.items.isNotEmpty()) {
            val firstItem = uiState.items[0]
            android.util.Log.d("DownloadsUI", "First item: name=${firstItem.name}, sources count=${firstItem.sources.size}")
        }
        
        // CRITICAL: Force hide all overlays
        binding.loadingIndicator.isVisible = false
        binding.loadingIndicator.visibility = android.view.View.GONE
        
        binding.errorLayout.errorPanel.isVisible = false
        binding.errorLayout.errorPanel.visibility = android.view.View.GONE
        
        binding.noDownloadsText.isVisible = false
        binding.noDownloadsText.visibility = android.view.View.GONE
        
        // CRITICAL: Force show RecyclerView
        binding.downloadsRecyclerView.isVisible = true
        binding.downloadsRecyclerView.visibility = android.view.View.VISIBLE
        binding.downloadsRecyclerView.bringToFront()
        
        android.util.Log.d("DownloadsUI", "Visibility set: RecyclerView=${binding.downloadsRecyclerView.visibility}, Loading=${binding.loadingIndicator.visibility}, Error=${binding.errorLayout.errorPanel.visibility}")
        
        val adapter = binding.downloadsRecyclerView.adapter as DownloadsAdapter
        android.util.Log.d("DownloadsUI", "Adapter before submitList: itemCount=${adapter.itemCount}")
        
        // Submit list - adapter will handle the rest
        adapter.submitList(uiState.items) {
            android.util.Log.d("DownloadsUI", "submitList completed: adapter.itemCount=${adapter.itemCount}, RecyclerView.childCount=${binding.downloadsRecyclerView.childCount}")
            
            // Force RecyclerView to layout and measure
            binding.downloadsRecyclerView.post {
                binding.downloadsRecyclerView.layoutManager?.requestLayout()
                binding.downloadsRecyclerView.invalidate()
                
                android.util.Log.d("DownloadsUI", "After forced layout: RecyclerView size=${binding.downloadsRecyclerView.width}x${binding.downloadsRecyclerView.height}, childCount=${binding.downloadsRecyclerView.childCount}")
            }
        }
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.downloadsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = false
        binding.noDownloadsText.isVisible = false
    }

    private fun bindUiStateError(uiState: DownloadsViewModel.UiState.Error) {
        binding.loadingIndicator.isVisible = false
        binding.downloadsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        binding.noDownloadsText.isVisible = false
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        // Use existing NavigationRoot routing conventions by delegating to the Activity
        try {
            (activity as? dev.jdtech.jellyfin.MainActivity)?.let { mainActivity ->
                mainActivity.navigateToItem(item)
                return
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to cast MainActivity for navigation")
        }
        // Fallback: no-op
    }
}
