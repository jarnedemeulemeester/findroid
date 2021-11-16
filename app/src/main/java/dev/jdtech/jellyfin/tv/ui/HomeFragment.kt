package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_DOWN_LEFT
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
internal class HomeFragment : BrowseSupportFragment() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        headersState = HEADERS_ENABLED
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
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

        viewModel.views().observe(viewLifecycleOwner) { homeItems ->
            rowsAdapter.clear()
            homeItems.map { section -> rowsAdapter.add(section.toListRow()) }
        }
    }

    private fun HomeItem.toListRow(): ListRow {
        return ListRow(
            toHeader(),
            toItems()
        )
    }

    private fun HomeItem.toHeader(): HeaderItem {
        return when (this) {
            is HomeItem.Section -> HeaderItem(homeSection.name)
            is HomeItem.ViewItem -> HeaderItem(
                String.format(
                    resources.getString(R.string.latest_library),
                    view.name
                )
            )
        }
    }

    private fun HomeItem.toItems(): ArrayObjectAdapter {
        return when (this) {
            is HomeItem.Section -> ArrayObjectAdapter(DynamicMediaItemPresenter { item ->
                navigateToMediaDetailFragment(item)
            }).apply { addAll(0, homeSection.items) }
            is HomeItem.ViewItem -> ArrayObjectAdapter(MediaItemPresenter { item ->
                navigateToMediaDetailFragment(item)
            }).apply { addAll(0, view.items) }
        }
    }

    private fun navigateToMediaDetailFragment(item: BaseItemDto) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToMediaDetailFragment(
                item.id,
                item.seriesName ?: item.name,
                item.type ?: "Unknown"
            )
        )
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToSettings()
        )
    }
}
