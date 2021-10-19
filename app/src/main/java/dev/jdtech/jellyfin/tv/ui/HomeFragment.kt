package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
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

        title = resources.getString(R.string.title_home)
        headersState = HEADERS_ENABLED
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.views.observe(viewLifecycleOwner) { homeItems ->
            homeItems.map { section -> rowsAdapter.add(section.toListRow()) }
        }
    }

    private fun HomeItem.toListRow(): ListRow {
        return ListRow(
            toHeader(),
            ArrayObjectAdapter(MediaItemPresenter()).apply { addAll(0, toItems()) }
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

    private fun HomeItem.toItems(): List<BaseItemDto> {
        return when (this) {
            is HomeItem.Section -> homeSection.items!!.map { it }
            is HomeItem.ViewItem -> view.items!!.map { it }
        }
    }
}
