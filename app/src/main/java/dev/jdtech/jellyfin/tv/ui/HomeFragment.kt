package dev.jdtech.jellyfin.tv.ui

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_DOWN_LEFT
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.fragments.HomeFragmentDirections
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber

@AndroidEntryPoint
internal class HomeFragment : BrowseSupportFragment() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var uiModeManager: UiModeManager

    private val adapterMap = mutableMapOf<String, ArrayObjectAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uiModeManager =
            requireContext().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager

        val rowPresenter = ListRowPresenter()
        rowPresenter.selectEffectEnabled = false

        headersState = HEADERS_ENABLED
        rowsAdapter = ArrayObjectAdapter(rowPresenter)
        adapter = rowsAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.settings).apply {
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KEYCODE_DPAD_DOWN || keyCode == KEYCODE_DPAD_DOWN_LEFT) {
                    headersSupportFragment.view?.requestFocus()
                    true
                } else {
                    false
                }
            }
            setOnClickListener { navigateToSettingsFragment() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is HomeViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is HomeViewModel.UiState.Loading -> bindUiStateLoading()
                        is HomeViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadData(includeLibraries = true)
            }
        }
    }

    private val diffCallbackListRow = object : DiffCallback<ListRow>() {
        override fun areItemsTheSame(oldItem: ListRow, newItem: ListRow): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ListRow, newItem: ListRow): Boolean {
            Timber.d((oldItem.adapter.size() == newItem.adapter.size()).toString())
            return oldItem.adapter.size() == newItem.adapter.size()
        }
    }

    private fun bindUiStateNormal(uiState: HomeViewModel.UiState.Normal) {
        progressBarManager.hide()
        uiState.apply {
            rowsAdapter.setItems(homeItems.map { homeItem -> homeItem.toListRow() }, diffCallbackListRow)
        }
    }

    private fun bindUiStateLoading() {
        progressBarManager.show()
    }

    private fun HomeItem.toListRow(): ListRow {
        return ListRow(
            toHeader(),
            toItems()
        )
    }

    private fun HomeItem.toHeader(): HeaderItem {
        return when (this) {
            is HomeItem.Libraries -> HeaderItem(section.name)
            is HomeItem.Section -> HeaderItem(homeSection.name)
            is HomeItem.ViewItem -> HeaderItem(
                String.format(
                    resources.getString(R.string.latest_library),
                    view.name
                )
            )
        }
    }

    val diffCallback = object : DiffCallback<BaseItemDto>() {
        override fun areItemsTheSame(oldItem: BaseItemDto, newItem: BaseItemDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BaseItemDto, newItem: BaseItemDto): Boolean {
            return oldItem == newItem
        }
    }

    private fun HomeItem.toItems(): ArrayObjectAdapter {
        val name = this.toHeader().name
        val items = when (this) {
            is HomeItem.Libraries -> section.items
            is HomeItem.Section -> homeSection.items
            is HomeItem.ViewItem -> view.items
        }
        if (name in adapterMap) {
            adapterMap[name]?.setItems(items, diffCallback)
        } else {
            adapterMap[name] = when (this) {
                is HomeItem.Libraries -> ArrayObjectAdapter(LibaryItemPresenter { item ->
                    navigateToLibraryFragment(item)
                }).apply { setItems(items, diffCallback) }
                is HomeItem.Section -> ArrayObjectAdapter(DynamicMediaItemPresenter { item ->
                    navigateToMediaDetailFragment(item)
                }).apply { setItems(items, diffCallback) }
                is HomeItem.ViewItem -> ArrayObjectAdapter(MediaItemPresenter { item ->
                    navigateToMediaDetailFragment(item)
                }).apply { setItems(items, diffCallback) }
            }
        }

        return adapterMap[name]!!
    }

    private fun navigateToLibraryFragment(library: BaseItemDto) {
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            findNavController().navigate(
                dev.jdtech.jellyfin.tv.ui.HomeFragmentDirections.actionHomeFragmentToLibraryFragment(
                    library.id,
                    library.name,
                    library.collectionType
                )
            )
        } else {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                    library.id,
                    library.name,
                    library.collectionType
                )
            )
        }
    }

    private fun navigateToMediaDetailFragment(item: BaseItemDto) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToMediaDetailFragment(
                item.id,
                item.seriesName ?: item.name,
                item.type
            )
        )
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
        )
    }
}
